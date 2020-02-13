/*
 * Copyright 2020 HM Revenue & Customs
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

import agent.auth.AgentJourneyState._
import agent.auth._
import agent.services._
import connectors.individual.eligibility.httpparsers.{Eligible, Ineligible}
import controllers.agent.ITSASessionKeys
import controllers.agent.ITSASessionKeys.FailedClientMatching
import core.config.BaseControllerConfig
import core.services.AuthService
import incometax.eligibility.services.GetEligibilityStatusService
import javax.inject.{Inject, Singleton}
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel}
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException
import usermatching.services.{LockoutUpdate, UserLockoutService}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.Left


@Singleton
class ConfirmClientController @Inject()(val baseConfig: BaseControllerConfig,
                                        val messagesApi: MessagesApi,
                                        val agentQualificationService: AgentQualificationService,
                                        val authService: AuthService,
                                        val lockOutService: UserLockoutService,
                                        val eligibilityService: GetEligibilityStatusService
                                       ) extends UserMatchingController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    views.html.agent.check_your_client_details(
      userDetailsModel,
      routes.ConfirmClientController.submit(),
      backUrl
    )

  private def withLockOutCheck(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]) = {
    (lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("ConfirmClientController.handleLockOut: " + e)
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

  private def handleFailedMatch(arn: String)(implicit request: Request[AnyContent]) = {
    val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
    lockOutService.incrementLockout(arn, currentCount).flatMap {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsErrorController.show())
          .addingToSession(FailedClientMatching -> newCount.toString))
      case Right(LockoutUpdate(_: LockedOut, _)) =>
        successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show())
          .removingFromSession(FailedClientMatching).clearUserDetails)
      case Left(failure) => failed(new InternalServerException("ConfirmClientController.lockUser: " + failure))
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
            Redirect(controllers.agent.routes.ClientAlreadySubscribedController.show())
              .removingFromSession(FailedClientMatching)
          )
          case Right(unapprovedAgent@UnApprovedAgent(clientNino, clientUtr)) =>
            successful(
              Redirect(controllers.agent.routes.NoClientRelationshipController.show())
                .removingFromSession(FailedClientMatching))
          case Right(ApprovedAgent(nino, Some(utr))) =>
            eligibilityService.getEligibilityStatus(utr) map {
              case Right(Eligible) =>
                Redirect(controllers.agent.routes.HomeController.index())
                  .withJourneyState(AgentUserMatched)
                  .addingToSession(ITSASessionKeys.NINO -> nino)
                  .addingToSession(ITSASessionKeys.UTR -> utr)
                  .removingFromSession(FailedClientMatching)
                  .clearUserDetails
              case Right(Ineligible) =>
                Redirect(controllers.agent.eligibility.routes.NotEligibleForIncomeTaxController.show())
                  .removingFromSession(FailedClientMatching)
                  .clearUserDetails
              case Left(error) =>
                throw new InternalServerException(s"Call to eligibility service failed with ${error.httpResponse}")
            }
          case Right(ApprovedAgent(nino, None)) =>
            Future.successful(Redirect(controllers.agent.routes.HomeController.index())
              .withJourneyState(AgentUserMatched)
              .addingToSession(ITSASessionKeys.NINO -> nino)
              .removingFromSession(FailedClientMatching)
              .clearUserDetails)
        }
      }
  }

  lazy val backUrl: String = routes.ClientDetailsController.show().url
}
