/*
 * Copyright 2021 HM Revenue & Customs
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
import config.AppConfig
import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.agent.ITSASessionKeys
import controllers.agent.ITSASessionKeys.FailedClientMatching
import javax.inject.{Inject, Singleton}
import models.audits.EnterDetailsAuditing
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.mvc._
import play.twirl.api.Html
import services._
import services.agent._
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left


@Singleton
class ConfirmClientController @Inject()(val auditingService: AuditingService,
                                        val authService: AuthService,
                                        agentQualificationService: AgentQualificationService,
                                        eligibilityService: GetEligibilityStatusService,
                                        lockOutService: UserLockoutService)
                                       (implicit val ec: ExecutionContext,
                                        val appConfig: AppConfig,
                                        mcc: MessagesControllerComponents) extends UserMatchingController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    views.html.agent.check_your_client_details(
      userDetailsModel,
      routes.ConfirmClientController.submit(),
      backUrl
    )

  private def withLockOutCheck(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]): Future[Result] = {
    lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show().url))
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

  private def handleFailedMatch(userDetails: UserDetailsModel, arn: String)(implicit request: Request[AnyContent]) = {
    val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
    lockOutService.incrementLockout(arn, currentCount).flatMap {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), userDetails, newCount, lockedOut = false))
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show())
          .addingToSession(FailedClientMatching -> newCount.toString))
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), userDetails, 0, lockedOut = true))
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show())
          .removingFromSession(FailedClientMatching).clearAllUserDetails)
      case _ => failed(new InternalServerException("ConfirmClientController.lockUser failure"))
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
            case Left(ClientAlreadySubscribed) =>
              auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
              successful(Redirect(controllers.agent.routes.ClientAlreadySubscribedController.show()).removingFromSession(FailedClientMatching))
            case Left(UnexpectedFailure) =>
              auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
              throw new InternalServerException("[ConfirmClientController][submit] orchestrate agent qualification failed with an unexpected failure")
            case Right(_: UnApprovedAgent) =>
              auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
              successful(Redirect(controllers.agent.routes.NoClientRelationshipController.show()).removingFromSession(FailedClientMatching))
            case Right(ApprovedAgent(nino, Some(utr))) =>
              auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
              eligibilityService.getEligibilityStatus(utr) map {
                case Right(Eligible) =>
                  Redirect(controllers.agent.routes.HomeController.index())
                    .withJourneyState(AgentUserMatched)
                    .addingToSession(ITSASessionKeys.NINO -> nino)
                    .addingToSession(ITSASessionKeys.UTR -> utr)
                    .removingFromSession(FailedClientMatching)
                    .clearUserDetailsExceptName
                case Right(Ineligible) =>
                  Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show())
                    .removingFromSession(FailedClientMatching)
                    .clearAllUserDetails
                case Left(error) =>
                  throw new InternalServerException(s"Call to eligibility service failed with ${error.httpResponse}")
              }
            case Right(ApprovedAgent(nino, None)) =>
              auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsAgent, Some(arn), clientDetails, currentCount, lockedOut = false))
              Future.successful(Redirect(controllers.agent.routes.HomeController.index())
                .withJourneyState(AgentUserMatched)
                .addingToSession(ITSASessionKeys.NINO -> nino)
                .removingFromSession(FailedClientMatching)
                .clearUserDetailsExceptName)
          }
          case None => successful(Redirect(routes.ClientDetailsController.show()))
        }
      }
  }

  lazy val backUrl: String = routes.ClientDetailsController.show().url
}
