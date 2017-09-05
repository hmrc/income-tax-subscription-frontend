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

package controllers.matching

import javax.inject.{Inject, Singleton}

import auth.{AuthenticatedController, IncomeTaxSAUser}
import config.BaseControllerConfig
import connectors.models.matching.{LockedOut, NotLockedOut}
import models.matching.UserDetailsModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future
import scala.util.Left

@Singleton
class ConfirmUserController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val authService: AuthService,
                                      val userMatching: UserMatchingService,
                                      val lockOutService: UserLockoutService
                                     ) extends AuthenticatedController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    views.html.check_your_user_details(
      userDetailsModel,
      routes.ConfirmUserController.submit(),
      backUrl
    )

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]) = {
    val bearerToken = implicitly[HeaderCarrier].token.get
    (lockOutService.getLockoutStatus(bearerToken.value) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(controllers.matching.routes.UserDetailsLockoutController.show().url))
    }).recover { case e =>
      throw new InternalServerException("user details controller: " + e)
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchUserDetails() map {
          case Some(userDetails) => Ok(view(userDetails))
          case _ => Redirect(routes.UserDetailsController.show())
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {

        import scala.concurrent.Future.{failed, successful}

        for {
          //todo redirect to user detail if it's not present
          clientDetails <- keystoreService.fetchUserDetails().collect { case Some(details) => details }
          client <- userMatching.matchClient(clientDetails)
        } yield client match {
          case Right(Some(user)) =>
            //TODO update session with nino and utr
            //todo check if they have both nino and utr
            Redirect(routes.UserDetailsController.show())
          case Right(None) =>
            //todo lockout mechanism
            Redirect(routes.UserDetailsController.show())
          case Left(_) => Redirect(routes.UserDetailsController.show())
        }// todo display error page for recovery

        //
        //        agentQualificationService.orchestrateAgentQualification(arn).flatMap {
        //          case Left(NoClientDetails) => successful(Redirect(routes.ClientDetailsController.show()))
        //          case Left(NoClientMatched) =>
        //            val currentCount = request.session.get(FailedClientMatching).fold(0)(_.toInt)
        //            val incCount = currentCount + 1
        //            if (incCount < applicationConfig.matchingAttempts) {
        //              successful(Redirect(controllers.matching.routes.ClientDetailsErrorController.show())
        //                .addingToSession(FailedClientMatching -> incCount.toString))
        //            }
        //            else {
        //              for {
        //                _ <- lockOutService.lockoutAgent(arn)
        //                _ <- keystoreService.deleteAll()
        //              } yield
        //                Redirect(controllers.matching.routes.ClientDetailsLockoutController.show()).removingFromSession(FailedClientMatching)
        //            }
        //          case Left(ClientAlreadySubscribed) => successful(Redirect(controllers.routes.ClientAlreadySubscribedController.show())
        //            .removingFromSession(FailedClientMatching))
        //          case Left(NoClientRelationship) => successful(Redirect(controllers.routes.NoClientRelationshipController.show())
        //            .removingFromSession(FailedClientMatching))
        //          case Right(_) => successful(Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
        //            .removingFromSession(FailedClientMatching))
        //        }.recoverWith {
        //          case e => failed(new InternalServerException("ConfirmClientController.submit\n" + e.getMessage))
        //        }
      }
  }

  lazy val backUrl: String = routes.UserDetailsController.show().url

}
