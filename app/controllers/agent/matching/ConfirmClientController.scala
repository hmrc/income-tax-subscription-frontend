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
import controllers.utils.ReferenceRetrieval
import models.{EligibilityStatus, PrePopData}
import models.audits.EnterDetailsAuditing
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.mvc._
import play.twirl.api.Html
import services._
import services.agent._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.HttpResult.HttpConnectorError
import views.html.agent.CheckYourClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientController @Inject()(val checkYourClientDetails: CheckYourClientDetails,
                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val agentQualificationService: AgentQualificationService,
                                        val getEligibilityStatusService: GetEligibilityStatusService,
                                        val mandationStatusService: MandationStatusService,
                                        val lockOutService: UserLockoutService,
                                        val prePopulationService: PrePopulationService,
                                        val subscriptionDetailsService: SubscriptionDetailsService)
                                       (implicit val ec: ExecutionContext,
                                        val appConfig: AppConfig,
                                        mcc: MessagesControllerComponents) extends UserMatchingController with ReferenceRetrieval with FeatureSwitching {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    checkYourClientDetails(
      userDetailsModel,
      routes.ConfirmClientController.submit()
    )

  private def withLockOutCheck(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]): Future[Result] = {
    lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show.url))
      case Left(_) => throw new InternalServerException("[ClientDetailsLockoutController][handleLockOut] lockout status failure")
    }
  }

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
        val arn = user.arn.get // Will fail if no ARN in user

        import ITSASessionKeys.FailedClientMatching

        val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)

        request.fetchUserDetails match {
          case Some(clientDetails) => agentQualificationService.orchestrateAgentQualification(clientDetails, arn).flatMap {
            case Left(NoClientMatched) => handleFailedMatch(clientDetails, arn)
            case Left(ClientAlreadySubscribed) => handleAlreadySubscribed(arn, currentCount, clientDetails)
            case Left(UnexpectedFailure) => handleFailure(arn, currentCount, clientDetails)
            case Right(_: UnApprovedAgent) => handleUnapproved(arn, currentCount, clientDetails)
            case Right(ApprovedAgent(nino, Some(utr))) => handleApprovedWithReference(arn, nino, currentCount, clientDetails, utr)
            case Right(ApprovedAgent(nino, None)) => handleApprovedWithoutReference(arn, currentCount, clientDetails, nino)
          }
          case None => successful(Redirect(routes.ClientDetailsController.show()))
        }
      }
  }

  private def handleFailedMatch(userDetails: UserDetailsModel, arn: String)(implicit request: Request[AnyContent]) = {
    val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
    lockOutService.incrementLockout(arn, currentCount).flatMap {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), userDetails, newCount, lockedOut = false))
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show)
          .addingToSession(FailedClientMatching -> newCount.toString))
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), userDetails, 0, lockedOut = true))
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show)
          .removingFromSession(FailedClientMatching).clearAllUserDetails)
      case _ => failed(new InternalServerException("ConfirmClientController.lockUser failure"))
    }
  }

  private def handleUnapproved(arn: String, currentCount: Int, clientDetails: UserDetailsModel)
                              (implicit request: Request[AnyContent]) = {
    auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
    successful(Redirect(controllers.agent.routes.NoClientRelationshipController.show).removingFromSession(FailedClientMatching))
  }

  private def handleApprovedWithoutReference(arn: String, currentCount: Int, clientDetails: UserDetailsModel, nino: String)
                                            (implicit request: Request[AnyContent]) = {
    auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
    Future.successful(Redirect(controllers.agent.routes.HomeController.index)
      .withJourneyState(AgentUserMatched)
      .addingToSession(ITSASessionKeys.NINO -> nino)
      .removingFromSession(FailedClientMatching)
      .clearUserDetailsExceptName)
  }

  private def handleApprovedWithReference(arn: String, nino: String, currentCount: Int, clientDetails: UserDetailsModel, utr: String)
                                         (implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      case Left(error: HttpConnectorError) =>
        throw new InternalServerException(s"Call to eligibility service failed with status: ${error.httpResponse.status}")
      case Right(result) =>
        withAgentReference(utr) { reference =>
          result match {
            case EligibilityStatus(false, nextYear, _) if !(nextYear && isEnabled(ControlListYears)) =>
              Future.successful(goToCannotTakePart)
            case EligibilityStatus(thisYear, _, prepopMaybe) =>
              for {
                _ <- handlePrepop(reference, prepopMaybe)
                _ <- handleMandationStatus(reference, nino, utr)
              } yield goToSignUpClient(thisYear, utr, nino)
          }
        }
    }
  }

  private def goToSignUpClient(thisYear: Boolean, utr: String, nino: String)(implicit request: Request[AnyContent]): Result =
    (if (thisYear)
      Redirect(controllers.agent.routes.HomeController.index)
    else
      Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show))
      .withJourneyState(AgentUserMatched)
      .addingToSession(ITSASessionKeys.NINO -> nino)
      .addingToSession(ITSASessionKeys.UTR -> utr)
      .addingToSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> (!thisYear).toString)
      .removingFromSession(FailedClientMatching)
      .clearUserDetailsExceptName


  private def goToCannotTakePart(implicit request: Request[AnyContent]): Result =
    Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)
      .removingFromSession(FailedClientMatching)
      .clearAllUserDetails

  private def handleFailure(arn: String, currentCount: Int, clientDetails: UserDetailsModel)
                           (implicit request: Request[AnyContent]): Future[Result] = {
    auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
    throw new InternalServerException("[ConfirmClientController][submit] orchestrate agent qualification failed with an unexpected failure")
  }

  private def handleAlreadySubscribed(arn: String, currentCount: Int, clientDetails: UserDetailsModel)
                                     (implicit request: Request[AnyContent]): Future[Result] = {
    auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
    successful(Redirect(controllers.agent.routes.ClientAlreadySubscribedController.show).removingFromSession(FailedClientMatching))
  }

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

  private def handleMandationStatus(reference: String, nino: String, utr: String)(implicit request: Request[AnyContent]): Future[Unit] = {
    if (isEnabled(ItsaMandationStatus)) {
      mandationStatusService.copyMandationStatus(reference, nino, utr)
    } else {
      Future.successful(())
    }
  }
}
