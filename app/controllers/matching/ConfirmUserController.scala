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
import connectors.models.matching.{LockedOut, NotLockedOut, UserMatchSuccessResponseModel}
import controllers.ITSASessionKeys._
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
        for {
        //todo redirect to user detail if it's not present
          clientDetails <- keystoreService.fetchUserDetails().collect { case Some(details) => details }
          client <- userMatching.matchClient(clientDetails)
          result <- client match {
            case Right(Some(userDetails)) =>
              userDetails match {
                case UserMatchSuccessResponseModel(_, _, _, nino, Some(utr)) =>
                  Future.successful(
                    Redirect(controllers.routes.HomeController.index())
                      .addingToSession(
                        NINO -> nino,
                        UTR -> utr
                      )
                      .removingFromSession(FailedUserMatching)
                  )
                case UserMatchSuccessResponseModel(_, _, _, nino, _) =>
                  Future.successful(
                    Redirect(controllers.routes.HomeController.index())
                      .addingToSession(NINO -> userDetails.nino)
                  )
              }
            case Right(None) =>
              val failedMatches = request.session.get(FailedUserMatching).fold(0)(_.toInt) + 1

              if (failedMatches < applicationConfig.matchingAttempts) {
                Future.successful(
                  Redirect(routes.UserDetailsErrorController.show())
                    .addingToSession(FailedUserMatching -> s"$failedMatches")
                )
              }
              else {
                val bearerToken = implicitly[HeaderCarrier].token.get
                for {
                  _ <- lockOutService.lockoutUser(bearerToken.value)
                } yield Redirect(routes.UserDetailsLockoutController.show())
                  .removingFromSession(FailedUserMatching)
              }
            case Left(_) =>
              Future.successful(Redirect(routes.UserDetailsController.show()))
          }
        } yield result // todo display error page for recovery
      }
  }

  lazy val backUrl: String = routes.UserDetailsController.show().url

}
