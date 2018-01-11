/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers.matching

import javax.inject.{Inject, Singleton}

import agent.auth.AgentJourneyState._
import agent.auth._
import agent.controllers.ITSASessionKeys
import agent.controllers.ITSASessionKeys.FailedClientMatching
import agent.services._
import core.auth.JourneyState
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException
import usermatching.models.{LockedOut, NotLockedOut, UserDetailsModel}
import usermatching.services.{LockoutUpdate, UserLockoutService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.Left


@Singleton
class ConfirmClientController @Inject()(val baseConfig: BaseControllerConfig,
                                        val messagesApi: MessagesApi,
                                        val agentQualificationService: AgentQualificationService,
                                        val authService: AuthService,
                                        val lockOutService: UserLockoutService
                                       ) extends UserMatchingController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    agent.views.html.check_your_client_details(
      userDetailsModel,
      routes.ConfirmClientController.submit(),
      backUrl
    )

  private def withLockOutCheck(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]) = {
    (lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("ConfirmClientController.handleLockOut: " + e)
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        request.fetchUserDetails match {
          case Some(clientDetails) => Future.successful(Ok(view(clientDetails)))
          case _ => Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsController.show()))
        }
      }
  }

  private def handleFailedMatch(arn: String)(implicit request: Request[AnyContent]) = {
    val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
    lockOutService.incrementLockout(arn, currentCount).flatMap {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        successful(Redirect(agent.controllers.matching.routes.ClientDetailsErrorController.show())
          .addingToSession(FailedClientMatching -> newCount.toString))
      case Right(LockoutUpdate(_: LockedOut, _)) =>
        successful(Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show())
          .removingFromSession(FailedClientMatching).clearUserDetails)
      case Left(failure) => failed(new InternalServerException("ConfirmClientControllerr.lockUser: " + failure))
    }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        val arn = user.arn.get // Will fail if no ARN in user

        import ITSASessionKeys.FailedClientMatching

        import scala.concurrent.Future.successful

        agentQualificationService.orchestrateAgentQualification(arn).flatMap {
          case Left(NoClientDetails) => successful(Redirect(routes.ClientDetailsController.show()))
          case Left(NoClientMatched) => handleFailedMatch(arn)
          case Left(ClientAlreadySubscribed) => successful(
            Redirect(agent.controllers.routes.ClientAlreadySubscribedController.show())
              .removingFromSession(FailedClientMatching)
          )
          case unapprovedAgent @ Right(UnApprovedAgent(clientNino, clientUtr)) =>
            if (applicationConfig.unauthorisedAgentEnabled) {
              successful(matched(unapprovedAgent.b,
                agent.controllers.routes.AgentNotAuthorisedController.show(), AgentUserMatching)
                .setAuthorisedAgent(false))
            } else {
              successful(
                Redirect(agent.controllers.routes.NoClientRelationshipController.show())
                  .removingFromSession(FailedClientMatching))
            }
          case approvedAgent @ Right(ApprovedAgent(nino, optUtr)) =>
            successful(matched(approvedAgent.b, agent.controllers.routes.HomeController.index(), AgentUserMatched)
              .clearUserDetails)
        }
      }
  }

  private def matched(qualifiedAgent: QualifiedAgent, call: Call, journeyState: AgentJourneyState)(implicit req: Request[_]): Result = {
    val resultWithoutUTR = Redirect(call)
      .withJourneyState(journeyState)
      .removingFromSession(FailedClientMatching)
      .addingToSession(ITSASessionKeys.NINO -> qualifiedAgent.clientNino)

    qualifiedAgent.clientUtr match {
      case Some(utr) => resultWithoutUTR.addingToSession(ITSASessionKeys.UTR -> utr)
      case _ => resultWithoutUTR.removingFromSession(ITSASessionKeys.UTR)
    }
  }

  lazy val backUrl: String = routes.ClientDetailsController.show().url
}
