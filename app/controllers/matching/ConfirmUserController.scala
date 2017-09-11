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
    val bearerToken = implicitly[HeaderCarrier].userId.get
    (lockOutService.getLockoutStatus(bearerToken) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(controllers.matching.routes.UserDetailsLockoutController.show().url))
    }).recover {
      case e => throw new InternalServerException("user details controller: " + e)
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchUserDetails() map {
          case Some(userDetails) => Ok(view(userDetails))
          case _ => Redirect(routes.UserDetailsLockoutController.show())
        }
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        keystoreService.fetchUserDetails() flatMap {
          case None =>
            Future.successful(Redirect(routes.UserDetailsController.show()))
          case Some(userDetails) =>
            matchUserDetails(userDetails)
        }
      }
  }

  private def matchUserDetails(userDetails: UserDetailsModel)(implicit request: Request[AnyContent]) = for {
    user <- userMatching.matchUser(userDetails)
    result <- user match {
      case Right(Some(matchedDetails)) => handleMatchedUser(matchedDetails)
      case Right(None) => handleFailedMatch
      case Left(error) => throw new InternalServerException(error.errors)
    }
  } yield result

  lazy val backUrl: String = routes.UserDetailsController.show().url

  private def handleFailedMatch(implicit request: Request[AnyContent]): Future[Result] = {
    val failedMatches = request.session.get(FailedUserMatching).fold(0)(_.toInt) + 1

    if (failedMatches < applicationConfig.matchingAttempts) {
      Future.successful(
        Redirect(routes.UserDetailsErrorController.show())
          .addingToSession(FailedUserMatching -> s"$failedMatches")
      )
    }
    else {
      val bearerToken = implicitly[HeaderCarrier].userId.get
      for {
        _ <- lockOutService.lockoutUser(bearerToken)
          .filter(_.isRight)
        _ <- keystoreService.deleteAll()
      } yield Redirect(routes.UserDetailsLockoutController.show())
        .removingFromSession(FailedUserMatching)
    }
  }

  private def handleMatchedUser(matchedDetails: UserMatchSuccessResponseModel)(implicit request: Request[AnyContent]): Future[Result] = {
    matchedDetails match {
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
            .addingToSession(NINO -> matchedDetails.nino)
        )
    }
  }
}

