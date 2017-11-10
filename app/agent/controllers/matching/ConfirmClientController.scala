/*
 * Copyright 2017 HM Revenue & Customs
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
import agent.auth.{AgentUserMatched, IncomeTaxAgentUser, UserMatchingController}
import agent.connectors.models.matching.{LockedOut, NotLockedOut}
import agent.controllers.ITSASessionKeys
import agent.services._
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException
import usermatching.models.UserDetailsModel

import scala.concurrent.Future
import scala.util.Left


@Singleton
class ConfirmClientController @Inject()(val baseConfig: BaseControllerConfig,
                                        val messagesApi: MessagesApi,
                                        val keystoreService: KeystoreService,
                                        val agentQualificationService: AgentQualificationService,
                                        //                                        val clientMatchingService: ClientMatchingService,
                                        //                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val lockOutService: AgentLockoutService
                                       ) extends UserMatchingController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    agent.views.html.check_your_client_details(
      userDetailsModel,
      routes.ConfirmClientController.submit(),
      backUrl
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]) = {
    (lockOutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("client details controller: " + e)
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchClientDetails() map {
          case Some(clientDetails) => Ok(view(clientDetails))
          case _ => Redirect(agent.controllers.matching.routes.ClientDetailsController.show())
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        val arn = user.arn.get // Will fail if no ARN in user

        import ITSASessionKeys.FailedClientMatching

        import scala.concurrent.Future.{failed, successful}

        agentQualificationService.orchestrateAgentQualification(arn).flatMap {
          case Left(NoClientDetails) => successful(Redirect(routes.ClientDetailsController.show()))
          case Left(NoClientMatched) =>
            val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
            val incCount = currentCount + 1
            if (incCount < applicationConfig.matchingAttempts) {
              successful(Redirect(agent.controllers.matching.routes.ClientDetailsErrorController.show())
                .addingToSession(FailedClientMatching -> incCount.toString))
            }
            else {
              for {
                _ <- lockOutService.lockoutAgent(arn)
                _ <- keystoreService.deleteAll()
              } yield
                Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show()).removingFromSession(FailedClientMatching)
            }
          case Left(ClientAlreadySubscribed) => successful(Redirect(agent.controllers.routes.ClientAlreadySubscribedController.show())
            .removingFromSession(FailedClientMatching))
          case Left(NoClientRelationship) => successful(Redirect(agent.controllers.routes.NoClientRelationshipController.show())
            .removingFromSession(FailedClientMatching))
          case Right(ApprovedAgent(nino, optUtr)) =>
            successful(
              {
                optUtr match {
                  case Some(_) =>
                    Redirect(agent.controllers.routes.HomeController.index())
                  case None =>
                    // TODO redirect to error page
                    NotImplemented
                }
              }.withJourneyState(AgentUserMatched).removingFromSession(FailedClientMatching)
            )
        }.recoverWith {
          case e => failed(new InternalServerException("ConfirmClientController.submit\n" + e.getMessage))
        }
      }
  }

  //
  //  private implicit class Util[A, B](first: Future[Either[A, B]]) {
  //    def flatMapRight(next: B => Future[Either[A, B]]): Future[Either[A, B]] =
  //      first.flatMap {
  //        case Right(v) => next(v)
  //        case left => successful(left)
  //      }
  //  }
  //
  //  def matchClient(arn: String)(implicit hc: HeaderCarrier): Future[Either[UnqualifiedAgent, ApprovedAgent]] = {
  //    val clientDetails: Future[Either[UnqualifiedAgent, ClientDetailsModel]] = keystoreService.fetchClientDetails()
  //      .collect {
  //        case Some(cd) => Right(cd)
  //        case _ => Left(NoClientDetails)
  //      }
  //      .recover { case _ => Left(UnexpectedFailure) }
  //
  //    clientDetails.flatMap {
  //      case Left(x) => successful(Left(x))
  //      case Right(cd) =>
  //        clientMatchingService.matchClient(cd)
  //          .collect {
  //            case Some(nino) =>
  //              auditingService.audit(
  //                ClientMatchingAuditModel(arn, cd, isSuccess = true),
  //                agent.controllers.matching.routes.ConfirmClientController.submit().url
  //              )
  //              Right(ApprovedAgent(nino))
  //            case None =>
  //              auditingService.audit(
  //                ClientMatchingAuditModel(arn, cd, isSuccess = false),
  //                agent.controllers.matching.routes.ConfirmClientController.submit().url
  //              )
  //              Left(NoClientMatched)
  //          }.recover { case _ => Left(UnexpectedFailure) }
  //    }
  //  }

  //  def submitSimplified(): Action[AnyContent] = Authenticated.async { implicit request =>
  //    implicit user =>
  //      handleLockOut {
  //        val arn = user.arn.get
  //        matchClient(arn) flatMap {
  //          case Left(NoClientDetails) => successful(Redirect(routes.ClientDetailsController.show()))
  //          case Left(NoClientMatched) =>
  //            val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
  //            val incCount = currentCount + 1
  //            if (incCount < applicationConfig.matchingAttempts) {
  //              successful(Redirect(agent.controllers.matching.routes.ClientDetailsErrorController.show())
  //                .addingToSession(FailedClientMatching -> incCount.toString))
  //            }
  //            else {
  //              for {
  //                _ <- lockOutService.lockoutAgent(arn)
  //                _ <- keystoreService.deleteAll()
  //              } yield
  //                Redirect(agent.controllers.matching.routes.ClientDetailsLockoutController.show()).removingFromSession(FailedClientMatching)
  //            }
  //          case Right(matched) => successful(Redirect(backUrl)) //TODO
  //        }
  //      }
  //  }

  lazy val backUrl: String = routes.ClientDetailsController.show().url

}
