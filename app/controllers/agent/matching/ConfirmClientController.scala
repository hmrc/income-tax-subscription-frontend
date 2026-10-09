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
import connectors.httpparser.SaveSessionDataHttpParser.{SaveSessionDataResponse, SaveSessionDataSuccessResponse}
import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataResponse
import controllers.SignUpBaseController
import controllers.agent.actions.{ClientDetailsJourneyRefiner, IdentifierAction}
import controllers.agent.resolvers.AlreadySignedUpResolver
import models.Channel
import models.agent.JourneyStep
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.requests.agent.IdentifierRequest
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.libs.json.Json
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
          case Left(ClientAlreadySubscribed(channel, utr, mtditid)) => handleClientAlreadySubscribed(clientDetails, channel, utr, mtditid)
          case Left(UnexpectedFailure) => handleUnexpectedFailure(clientDetails)
          case Left(UnApprovedAgent(nino, _)) => handleUnapprovedAgent(nino, clientDetails)
          case Right(ApprovedAgent(nino, None)) => handleApprovedAgentWithoutClientUTR(nino, clientDetails)
          case Right(ApprovedAgent(nino, Some(utr))) => handleApprovedAgent(nino, utr, clientDetails)
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

  private def auditDetailsEntered(clientDetails: UserDetailsModel, numberOfAttempts: Int, lockedOut: Boolean)
                                 (implicit request: IdentifierRequest[_]): Unit = {
    auditingService.audit(EnterDetailsAuditModel(
      agentReferenceNumber = request.arn,
      userDetails = clientDetails,
      numberOfAttempts = numberOfAttempts,
      lockedOut = lockedOut
    ))
  }

  private def getCurrentFailureCount()(implicit request: IdentifierRequest[_]): Future[Int] = {
    sessionDataService.fetchFailedClientMatching().map(_.getOrElse(0))
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

    getCurrentFailureCount().flatMap { currentFailureCount =>

      lockOutService.incrementLockout(request.arn, currentFailureCount).flatMap {

        case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
          auditDetailsEntered(clientDetails, newCount, lockedOut = false)

          auditingService.audit(EligibilityAuditModel(
            agentReferenceNumber = Some(request.arn),
            utr = None,
            nino = None,
            eligibility = "ineligible",
            failureReason = Some("failed-client-match-no-lock-out")
          ))


          sessionDataService.saveFailedClientMatching(newCount).map {
            case Right(_) => Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show)
            case Left(_) => throw new InternalServerException("handleFailedClientMatch - failure when saving failed client matching")
          }

        case Right(LockoutUpdate(_: LockedOut, None)) =>
          auditDetailsEntered(clientDetails, 0, lockedOut = true)

          auditingService.audit(EligibilityAuditModel(
            agentReferenceNumber = Some(request.arn),
            utr = None,
            nino = None,
            eligibility = "ineligible",
            failureReason = Some("failed-client-match-locked-out")
          ))

          sessionDataService.deleteFailedClientMatching().map {
            case Right(_) => Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show).clearAllUserDetails
            case Left(_) => throw new InternalServerException("handleFailedClientMatch - failure when deleting failed client matching")
          }
        case _ => throw new InternalServerException("ConfirmClientController.lockUser failure")
      }
    }
  }

  private def handleClientAlreadySubscribed(clientDetails: UserDetailsModel, reason: Option[Channel], utr: Option[String], mtditid: String)
                                           (implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    getCurrentFailureCount().flatMap { currentFailureCount =>
      auditDetailsEntered(clientDetails, currentFailureCount, lockedOut = false)
      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = Some(request.arn),
        utr = None,
        nino = Some(clientDetails.nino),
        eligibility = "ineligible",
        failureReason = Some("client-already-signed-up")
      ))
      sessionDataService.saveNino(clientDetails.nino) flatMap {
        case Right(_) =>
          utr.fold[Future[SaveSessionDataResponse]](Future.successful(Right(SaveSessionDataSuccessResponse)))(sessionDataService.saveUTR) flatMap {
            case Right(_) =>
              sessionDataService.saveMTDITID(mtditid) flatMap {
                case Right(_) =>

                  resolver.resolve(
                    sessionData = request.sessionData.copy(data = request.sessionData.data + (ITSASessionKeys.NINO -> Json.toJson(clientDetails.nino))),
                    channel = reason
                  ).flatMap { result =>
                    sessionDataService.deleteFailedClientMatching().map {
                      case Right(_) => result
                      case Left(_) => throw new InternalServerException("handleClientAlreadySubscribed - failed ")
                    }

                  }

                case Left(_) =>
                  throw new InternalServerException("[ConfirmClientController][handleClientAlreadySubscribed] - failure when saving mtditid to session")
              }
            case Left(_) =>
              throw new InternalServerException("[ConfirmClientController][handleClientAlreadySubscribed] - failure when saving utr to session")
          }
        case Left(_) =>
          throw new InternalServerException("[ConfirmClientController][handleClientAlreadySubscribed] - failure when saving nino to session")
      }
    }
  }

  private def handleUnexpectedFailure(clientDetails: UserDetailsModel)
                                     (implicit request: IdentifierRequest[_]): Future[Result] = {

    getCurrentFailureCount().flatMap { currentFailureCount =>
      auditDetailsEntered(clientDetails, currentFailureCount, lockedOut = false)

      throw new InternalServerException("[ConfirmClientController][handleUnexpectedFailure] - orchestrate agent qualification failed with an unexpected failure")
    }
  }

  private def handleUnapprovedAgent(nino: String, clientDetails: UserDetailsModel)
                                   (implicit request: IdentifierRequest[_]): Future[Result] = {
    getCurrentFailureCount().flatMap { currentFailureCount =>

      auditDetailsEntered(clientDetails, currentFailureCount, lockedOut = false)

      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = Some(request.arn),
        utr = None,
        nino = Some(clientDetails.nino),
        eligibility = "ineligible",
        failureReason = Some("no-agent-client-relationship")
      ))

      sessionDataService.saveNino(nino) flatMap {
        case Right(_) =>
          sessionDataService.saveJourneyStep(JourneyStep.SignPosted).flatMap {
            case Right(_) =>
              sessionDataService.deleteFailedClientMatching().map {
                case Right(_) => Redirect(controllers.agent.matching.routes.NoClientRelationshipController.show)

                case Left(_) => throw new InternalServerException("[ConfirmClientController][handleUnapprovedAgent] - failure when deleting failed client matching from session")
              }
            case Left(_) =>
              throw new InternalServerException("failure when saving journey step to session")
          }
        case Left(_) =>
          throw new InternalServerException("[ConfirmClientController][handleUnapprovedAgent] - failure when saving nino to session")
      }
    }
  }

  private def handleApprovedAgentWithoutClientUTR(nino: String, clientDetails: UserDetailsModel)
                                                 (implicit request: IdentifierRequest[_]): Future[Result] = {
    getCurrentFailureCount().flatMap { currentFailureCount =>

      auditDetailsEntered(clientDetails, currentFailureCount, lockedOut = false)

      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = Some(request.arn),
        utr = None,
        nino = Some(nino),
        eligibility = "ineligible",
        failureReason = Some("no-self-assessment")
      ))

      sessionDataService.deleteFailedClientMatching().map {
        case Right(_) =>
          Redirect(controllers.agent.matching.routes.NoSAController.show)
        case Left(_) =>
          throw new InternalServerException("[ConfirmClientController][handleApprovedAgentWithoutClientUTR] - failure when deleting  failed client matching from session")
      }
    }
  }

    private def handleApprovedAgent(nino: String, utr: String, clientDetails: UserDetailsModel)
                                   (implicit request: IdentifierRequest[_]): Future[Result] = {

      getCurrentFailureCount().flatMap { currentFailureCount =>

        auditDetailsEntered(clientDetails, currentFailureCount, lockedOut = false)

        sessionDataService.saveNino(nino).flatMap {
          case Right(_) =>
            sessionDataService.saveUTR(utr).flatMap {
              case Right(_) =>
                sessionDataService.deleteFailedClientMatching().map {
                  case Right(_) => Redirect(routes.ConfirmedClientResolver.resolve)
                  case Left(_) => throw new InternalServerException("[ConfirmClientController][handleApprovedAgentWithoutClientUTR] - failure when deleting  failed client matching from session")
                }
              case Left(_) =>
                throw new InternalServerException("[ConfirmClientController][handleApprovedAgent] - failure when saving utr to session")
            }
          case Left(_) =>
            throw new InternalServerException("[ConfirmClientController][handleApprovedAgent] - failure when saving nino to session")
        }
      }
    }
  }
