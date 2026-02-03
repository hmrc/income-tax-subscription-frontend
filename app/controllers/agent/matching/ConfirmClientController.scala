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

import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.FailedClientMatching
import controllers.SignUpBaseController
import controllers.agent.actions.{ClientDetailsJourneyRefiner, IdentifierAction}
import controllers.agent.resolvers.AlreadySignedUpResolver
import models.Channel
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.requests.agent.IdentifierRequest
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.mvc.*
import play.twirl.api.Html
import services.*
import services.agent.*
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.{UserMatchingSessionRequestUtil, UserMatchingSessionResultUtil}
import views.html.agent.matching.CheckYourClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientController @Inject()(identify: IdentifierAction,
                                        journeyRefiner: ClientDetailsJourneyRefiner,
                                        auditingService: AuditingService,
                                        checkYourClientDetails: CheckYourClientDetails,
                                        agentQualificationService: AgentQualificationService,
                                        sessionDataService: SessionDataService,
                                        resolver: AlreadySignedUpResolver,
                                        lockOutService: UserLockoutService)
                                       (implicit ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withLockOutCheck {
      request.fetchUserDetails match {
        case Some(clientDetails) => Future.successful(Ok(view(clientDetails)))
        case _ => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show()))
      }
    }
  }

  def submit(): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withLockOutCheck {
      withClientDetails { clientDetails =>
        agentQualificationService.orchestrateAgentQualification(clientDetails, request.arn) flatMap {
          case Left(NoClientMatched) => handleFailedClientMatch(clientDetails)
          case Left(ClientAlreadySubscribed(channel)) => handleClientAlreadySubscribed(clientDetails, channel)
          case Left(UnexpectedFailure) => Future.successful(handleUnexpectedFailure(clientDetails))
          case Left(UnApprovedAgent(nino, _)) => handleUnapprovedAgent(nino, clientDetails)
          case Right(ApprovedAgent(nino, None)) => Future.successful(handleApprovedAgentWithoutClientUTR(nino, clientDetails))
          case Right(ApprovedAgent(nino, Some(utr))) => handleApprovedAgent(nino, utr, clientDetails)
        }
      }
    }
  }

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html = {
    checkYourClientDetails(
      userDetailsModel,
      routes.ConfirmClientController.submit(),
      backUrl = backUrl
    )
  }

  private def auditDetailsEntered(clientDetails: UserDetailsModel, numberOfAttempts: Int, lockedOut: Boolean)
                                 (implicit request: IdentifierRequest[_]): Unit = {
    auditingService.audit(EnterDetailsAuditModel(
      agentReferenceNumber = request.arn,
      userDetails = clientDetails,
      numberOfAttempts = numberOfAttempts,
      lockedOut = lockedOut
    ))
  }

  private def getCurrentFailureCount()(implicit request: IdentifierRequest[_]): Int = {
    request.session.get(FailedClientMatching).fold(0)(_.toInt)
  }

  private def withLockOutCheck(f: => Future[Result])
                              (implicit request: IdentifierRequest[_]): Future[Result] = {
    lockOutService.getLockoutStatus(request.arn) flatMap {
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

  private def handleFailedClientMatch(clientDetails: UserDetailsModel)
                                     (implicit request: IdentifierRequest[_]): Future[Result] = {
    val currentFailureCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)

    lockOutService.incrementLockout(request.arn, currentFailureCount) map {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        auditDetailsEntered(clientDetails, newCount, lockedOut = false)
        auditingService.audit(EligibilityAuditModel(
          agentReferenceNumber = Some(request.arn),
          utr = None,
          nino = None,
          eligibility = "ineligible",
          failureReason = Some("failed-client-match-no-lock-out")
        ))
        Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show)
          .addingToSession(FailedClientMatching -> newCount.toString)
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditDetailsEntered(clientDetails, 0, lockedOut = true)
        auditingService.audit(EligibilityAuditModel(
          agentReferenceNumber = Some(request.arn),
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

  private def handleClientAlreadySubscribed(clientDetails: UserDetailsModel, reason: Option[Channel])
                                           (implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    auditDetailsEntered(clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(request.arn),
      utr = None,
      nino = Some(clientDetails.nino),
      eligibility = "ineligible",
      failureReason = Some("client-already-signed-up")
    ))
    resolver.resolve(request.sessionData, reason).map(_.removingFromSession(FailedClientMatching))
  }

  private def handleUnexpectedFailure(clientDetails: UserDetailsModel)
                                     (implicit request: IdentifierRequest[_]): Result = {
    auditDetailsEntered(clientDetails, getCurrentFailureCount(), lockedOut = false)
    throw new InternalServerException("[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure")
  }

  private def handleUnapprovedAgent(nino: String, clientDetails: UserDetailsModel)
                                   (implicit request: IdentifierRequest[_]): Future[Result] = {
    auditDetailsEntered(clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(request.arn),
      utr = None,
      nino = Some(clientDetails.nino),
      eligibility = "ineligible",
      failureReason = Some("no-agent-client-relationship")
    ))
    sessionDataService.saveNino(nino) map {
      case Right(_) => Redirect(controllers.agent.matching.routes.NoClientRelationshipController.show)
        .addingToSession(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true")
        .removingFromSession(FailedClientMatching)
      case Left(_) => throw new InternalServerException("[ConfirmClientController][handleUnapprovedAgent] - failure when saving nino to session")
    }
  }

  private def handleApprovedAgentWithoutClientUTR(nino: String, clientDetails: UserDetailsModel)
                                                 (implicit request: IdentifierRequest[_]): Result = {
    auditDetailsEntered(clientDetails, getCurrentFailureCount(), lockedOut = false)
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(request.arn),
      utr = None,
      nino = Some(nino),
      eligibility = "ineligible",
      failureReason = Some("no-self-assessment")
    ))
    Redirect(controllers.agent.matching.routes.NoSAController.show)
      .removingFromSession(FailedClientMatching)
  }

  private def handleApprovedAgent(nino: String, utr: String, clientDetails: UserDetailsModel)
                                 (implicit request: IdentifierRequest[_]): Future[Result] = {
    auditDetailsEntered(clientDetails, getCurrentFailureCount(), lockedOut = false)
    sessionDataService.saveNino(nino) flatMap {
      case Right(_) =>
        sessionDataService.saveUTR(utr) map {
          case Right(_) =>
            Redirect(routes.ConfirmedClientResolver.resolve)
              .addingToSession(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true")
              .removingFromSession(FailedClientMatching)
          case Left(_) => throw new InternalServerException("[ConfirmClientController][handleApprovedAgent] - failure when saving utr to session")
        }
      case Left(_) => throw new InternalServerException("[ConfirmClientController][handleApprovedAgent] - failure when saving nino to session")
    }

  }

  def backUrl: String = {
    controllers.agent.matching.routes.ClientDetailsController.show().url
  }
}
