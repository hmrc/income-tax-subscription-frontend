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

import auth.agent.{IncomeTaxAgentUser, UserMatchingController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.FailedClientMatching
import config.AppConfig
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.mvc._
import play.twirl.api.Html
import services._
import services.agent._
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.matching.CheckYourClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientController @Inject()(checkYourClientDetails: CheckYourClientDetails,
                                        agentQualificationService: AgentQualificationService,
                                        lockOutService: UserLockoutService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val sessionDataService: SessionDataService,
                                        val appConfig: AppConfig,
                                        val subscriptionDetailsService: SubscriptionDetailsService)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends UserMatchingController {

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
            case Left(_: UnApprovedAgent) => Future.successful(handleUnapprovedAgent(user.arn, clientDetails))
            case Right(ApprovedAgent(_, None)) => Future.successful(handleApprovedAgentWithoutClientUTR(user.arn, clientDetails))
            case Right(ApprovedAgent(_, Some(utr))) => handleApprovedAgent(user.arn, utr, clientDetails)
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
        auditingService.audit(EligibilityAuditModel(
          agentReferenceNumber = Some(arn),
          utr = None,
          nino = None,
          eligibility = "ineligible",
          failureReason = Some("failed-client-match-no-lock-out")
        ))
        Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show)
          .addingToSession(FailedClientMatching -> newCount.toString)
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditDetailsEntered(arn, clientDetails, 0, lockedOut = true)
        auditingService.audit(EligibilityAuditModel(
          agentReferenceNumber = Some(arn),
          utr = None,
          nino = None,
          eligibility = "ineligible",
          failureReason = Some("failed-client-match-locked-out")
        ))
        Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show)
          .removingFromSession(FailedClientMatching)
          .clearAllUserDetails
      case _ => throw new InternalServerException("ConfirmClientController.lockUser failure")
    }
  }

  private def handleClientAlreadySubscribed(arn: String, clientDetails: UserDetailsModel)
                                           (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(arn),
      utr = None,
      nino = Some(clientDetails.nino),
      eligibility = "ineligible",
      failureReason = Some("client-already-signed-up")
    ))
    Redirect(controllers.agent.matching.routes.ClientAlreadySubscribedController.show).removingFromSession(FailedClientMatching)
  }

  private def handleUnexpectedFailure(arn: String, clientDetails: UserDetailsModel)
                                     (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    throw new InternalServerException("[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure")
  }

  private def handleUnapprovedAgent(arn: String, clientDetails: UserDetailsModel)
                                   (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(arn),
      utr = None,
      nino = Some(clientDetails.nino),
      eligibility = "ineligible",
      failureReason = Some("no-agent-client-relationship")
    ))
    Redirect(controllers.agent.matching.routes.NoClientRelationshipController.show).removingFromSession(FailedClientMatching)
  }

  private def handleApprovedAgentWithoutClientUTR(arn: String, clientDetails: UserDetailsModel)
                                                 (implicit request: Request[AnyContent]): Result = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(arn),
      utr = None,
      nino = Some(clientDetails.nino),
      eligibility = "ineligible",
      failureReason = Some("no-self-assessment")
    ))
    Redirect(controllers.agent.matching.routes.NoSAController.show)
      .removingFromSession(FailedClientMatching)
  }

  private def handleApprovedAgent(arn: String, utr: String, clientDetails: UserDetailsModel)
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    auditDetailsEntered(arn, clientDetails, getCurrentFailureCount(), lockedOut = false)
    sessionDataService.saveNino(clientDetails.nino) map {
      case Right(_) =>
        Redirect(routes.ConfirmedClientResolver.resolve)
          .addingToSession(ITSASessionKeys.UTR -> utr)
          .removingFromSession(FailedClientMatching)
          .clearUserDetailsExceptName
      case Left(_) => throw new InternalServerException("[ConfirmClientController][handleApprovedAgent] - failure when saving nino to session")
    }

  }

}
