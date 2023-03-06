/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.agent.matching

import auth.agent.AgentJourneyState._
import auth.agent.{AgentUserMatched, IncomeTaxAgentUser, UserMatchingController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.FailedClientMatching
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ControlListYears, ItsaMandationStatus, PrePopulate}
import config.featureswitch.FeatureSwitching
import connectors.MandationStatusConnector
import controllers.utils.ReferenceRetrieval
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import models.{EligibilityStatus, PrePopData}
import play.api.mvc._
import play.twirl.api.Html
import services._
import services.agent._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.CheckYourClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientController @Inject()(val checkYourClientDetails: CheckYourClientDetails,
                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val agentQualificationService: AgentQualificationService,
                                        val getEligibilityStatusService: GetEligibilityStatusService,
                                        val mandationStatusConnector: MandationStatusConnector,
                                        val lockOutService: UserLockoutService,
                                        val prePopulationService: PrePopulationService,
                                        val subscriptionDetailsService: SubscriptionDetailsService)
                                       (implicit val ec: ExecutionContext,
                                        val appConfig: AppConfig,
                                        mcc: MessagesControllerComponents) extends UserMatchingController with ReferenceRetrieval with FeatureSwitching {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        request.fetchUserDetails match {
          case Some(clientDetails) => Future.successful(Ok(view(clientDetails)))
          case _ => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show()))
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        withClientDetails { clientDetails =>
          agentQualificationService.orchestrateAgentQualification(clientDetails, user.arn) flatMap {
            case Left(NoClientMatched) => handleFailedClientMatch(user.arn, clientDetails)
            case Left(ClientAlreadySubscribed) => Future.successful(handleClientAlreadySubscribed(user.arn, clientDetails))
            case Left(UnexpectedFailure) => Future.successful(handleUnexpectedFailure(user.arn, clientDetails))
            case Right(_: UnApprovedAgent) => Future.successful(handleUnapprovedAgent(user.arn, clientDetails))
            case Right(ApprovedAgent(nino, None)) => Future.successful(handleApprovedAgentWithoutClientUTR(user.arn, nino, clientDetails))
            case Right(ApprovedAgent(nino, Some(utr))) => handleApprovedAgent(user.arn, nino, utr, clientDetails)
          }
        }
      }
  }

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html = {
    checkYourClientDetails(
      userDetailsModel,
      routes.ConfirmClientController.submit()
    )
  }

  private def auditDetailsEntered(arn: String, clientDetails: UserDetailsModel, numberOfAttempts: Int, lockedOut: Boolean)
                                 (implicit request: Request[AnyContent]): Unit = {
    auditingService.audit(EnterDetailsAuditModel(
      agentReferenceNumber = arn,
      userDetails = clientDetails,
      numberOfAttempts = numberOfAttempts,
      lockedOut = lockedOut
    ))
  }

  private def getCurrentFailureCount()(implicit request: Request[AnyContent]): Int = {
    request.session.get(FailedClientMatching).fold(0)(_.toInt)
  }

  private def withLockOutCheck(f: => Future[Result])
                              (implicit user: IncomeTaxAgentUser, request: Request[_]): Future[Result] = {
    lockOutService.getLockoutStatus(user.arn) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show.url))
      case Left(_) => throw new InternalServerException("[ClientDetailsLockoutController][handleLockOut] lockout status failure")
    }
  }

  private def withClientDetails(f: UserDetailsModel => Future[Result])
                               (implicit request: Request[AnyContent]): Future[Result] = {
    request.fetchUserDetails match {
      case Some(clientDetails) => f(clientDetails)
      case None => Future.successful(Redirect(routes.ClientDetailsController.show()))
    }
  }

  private def handleFailedClientMatch(arn: String, clientDetails: UserDetailsModel)
                                     (implicit request: Request[AnyContent]): Future[Result] = {
    val currentFailureCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)

    lockOutService.incrementLockout(arn, currentFailureCount) map {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        auditDetailsEntered(arn, clientDetails, newCount, lockedOut = false)
        Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show)
          .addingToSession(FailedClientMatching -> newCount.toString)
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditDetailsEntered(arn, clientDetails, 0, lockedOut = true)
        Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show)
          .removingFromSession(FailedClientMatching)
          .clearAllUserDetails
      case _ => throw new InternalServerException("ConfirmClientController.lockUser failure")
    }
  }

  private def handleClientAlreadySubscribed(arn: String, clientDetails: UserDetailsModel)
                                           (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    Redirect(controllers.agent.routes.ClientAlreadySubscribedController.show).removingFromSession(FailedClientMatching)
  }

  private def handleUnexpectedFailure(arn: String, clientDetails: UserDetailsModel)
                                     (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    throw new InternalServerException("[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure")
  }

  private def handleUnapprovedAgent(arn: String, clientDetails: UserDetailsModel)
                                   (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    Redirect(controllers.agent.routes.NoClientRelationshipController.show).removingFromSession(FailedClientMatching)
  }

  private def handleApprovedAgentWithoutClientUTR(arn: String, nino: String, clientDetails: UserDetailsModel)
                                                 (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    Redirect(controllers.agent.routes.HomeController.index)
      .withJourneyState(AgentUserMatched)
      .addingToSession(ITSASessionKeys.NINO -> nino)
      .removingFromSession(FailedClientMatching)
      .clearUserDetailsExceptName
  }

  private def withEligibilityResult(utr: String)(f: EligibilityStatus => Future[Result])
                                   (implicit request: Request[AnyContent]): Future[Result] = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      case Left(value) =>
        throw new InternalServerException(
          s"[ConfirmClientController][withEligibilityResult] - call to control list failed with status: ${value.httpResponse.status}"
        )
      case Right(result) =>
        f(result)
    }
  }

  private def withMandationStatus(nino: String, utr: String)
                                 (f: MandationStatusModel => Result)
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    if (isEnabled(ItsaMandationStatus)) {
      mandationStatusConnector.getMandationStatus(nino, utr) map {
        case Left(_) =>
          throw new InternalServerException("[ConfirmClientController][withMandationStatus] - Unexpected failure when receiving mandation status")
        case Right(model) =>
          f(model)
      }
    } else {
      Future.successful(f(MandationStatusModel(Voluntary, Voluntary)))
    }
  }

  private def handleApprovedAgent(arn: String, nino: String, utr: String, clientDetails: UserDetailsModel)
                                 (implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    withEligibilityResult(utr) { eligibilityResult =>
      withAgentReference(utr) { reference =>
        eligibilityResult match {
          case EligibilityStatus(false, nextYear, _) if !(nextYear && isEnabled(ControlListYears)) =>
            Future.successful(
              goToCannotTakePart
                .removingFromSession(FailedClientMatching)
                .clearAllUserDetails
            )
          case EligibilityStatus(thisYear, _, prepop) =>
            handlePrepop(reference, prepop) flatMap { _ =>
              withMandationStatus(nino, utr) { mandationStatus =>
                goToSignUpClient(thisYear)
                  .withJourneyState(AgentUserMatched)
                  .addingToSession(ITSASessionKeys.NINO -> nino)
                  .addingToSession(ITSASessionKeys.UTR -> utr)
                  .addingToSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> (!thisYear).toString)
                  .addingToSession(ITSASessionKeys.MANDATED_CURRENT_YEAR -> (mandationStatus.currentYearStatus == Mandated).toString)
                  .addingToSession(ITSASessionKeys.MANDATED_NEXT_YEAR -> (mandationStatus.nextYearStatus == Mandated).toString)
                  .removingFromSession(FailedClientMatching)
                  .clearUserDetailsExceptName
              }
            }
        }
      }
    }
  }

  private def goToSignUpClient(thisYear: Boolean): Result = {
    if (thisYear) {
      Redirect(controllers.agent.routes.HomeController.index)
    } else {
      Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
    }
  }


  private def goToCannotTakePart: Result =
    Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

}
