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

package controllers.usermatching

import auth.individual.JourneyState.ResultFunctions
import auth.individual.{IncomeTaxSAUser, UserMatched, UserMatchingController}
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.audits.EnterDetailsAuditing
import models.audits.EnterDetailsAuditing.EnterDetailsAuditModel
import models.usermatching.{LockedOut, NotLockedOut, UserDetailsModel, UserMatchSuccessResponseModel}
import play.api.mvc._
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.ITSASessionKeys._

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left

@Singleton
class ConfirmUserController @Inject()(val auditingService: AuditingService,
                                      val authService: AuthService,
                                      lockOutService: UserLockoutService,
                                      userMatching: UserMatchingService)
                                     (implicit val ec: ExecutionContext,
                                      val appConfig: AppConfig,
                                      mcc: MessagesControllerComponents) extends UserMatchingController {

  def view(userDetailsModel: UserDetailsModel)(implicit request: Request[_]): Html =
    views.html.individual.usermatching.check_your_user_details(
      userDetailsModel,
      controllers.usermatching.routes.ConfirmUserController.submit(),
      backUrl
    )

  private def withLockOutCheck(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]): Future[Result] = {
    lockOutService.getLockoutStatus(user.userId) flatMap {
      case Right(NotLockedOut) => f
      case Right(_: LockedOut) =>
        Future.successful(Redirect(controllers.usermatching.routes.UserDetailsLockoutController.show().url))
      case Left(_) => throw new InternalServerException("[ConfirmUserController][withLockOutCheck] received failure response from lockout service")
    }
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        Future.successful(
          request.fetchUserDetails match {
            case Some(userDetails) => Ok(view(userDetails))
            case _ => Redirect(controllers.usermatching.routes.UserDetailsController.show())
          }
        )
      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withLockOutCheck {
        request.fetchUserDetails match {
          case None =>
            Future.successful(Redirect(controllers.usermatching.routes.UserDetailsController.show()))
          case Some(userDetails) =>
            matchUserDetails(userDetails)
        }
      }
  }

  private def handleFailedMatch(userDetails: UserDetailsModel, bearerToken: String)(implicit request: Request[AnyContent]): Future[Result] = {
    val currentCount = request.session.get(FailedUserMatching).fold(0)(_.toInt)
    lockOutService.incrementLockout(bearerToken, currentCount).flatMap {
      case Right(LockoutUpdate(NotLockedOut, Some(newCount))) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, newCount, lockedOut = false))
        successful(Redirect(controllers.usermatching.routes.UserDetailsErrorController.show())
          .addingToSession(FailedUserMatching -> newCount.toString))
      case Right(LockoutUpdate(_: LockedOut, None)) =>
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, 0, lockedOut = true))
        successful(Redirect(controllers.usermatching.routes.UserDetailsLockoutController.show())
          .removingFromSession(FailedUserMatching))
      case _ => failed(new InternalServerException("ConfirmUserController.lockUser"))
    }
  }

  private def matchUserDetails(userDetails: UserDetailsModel)(implicit request: Request[AnyContent], saUser: IncomeTaxSAUser): Future[Result] = for {
    user <- userMatching.matchUser(userDetails)
    result <- user match {
      case Right(Some(matchedDetails)) =>
        val currentCount = request.session.get(FailedUserMatching).fold(0)(_.toInt)
        auditingService.audit(EnterDetailsAuditModel(EnterDetailsAuditing.enterDetailsIndividual, None, userDetails, currentCount, lockedOut = false))
        handleMatchedUser(matchedDetails)
      case Right(None) => handleFailedMatch(userDetails, saUser.userId)
      case Left(error) => throw new InternalServerException(error.errors)
    }
  } yield result

  lazy val backUrl: String = controllers.usermatching.routes.UserDetailsController.show().url

  private def handleMatchedUser(matchedDetails: UserMatchSuccessResponseModel)
                               (implicit request: Request[AnyContent]): Future[Result] = {
    matchedDetails match {
      case UserMatchSuccessResponseModel(_, _, _, nino, Some(utr)) =>
        Future.successful(
          Redirect(routes.HomeController.index())
            .addingToSession(
              NINO -> nino,
              UTR -> utr
            )
        )
      case UserMatchSuccessResponseModel(_, _, _, nino, _) =>
        Future.successful(
          Redirect(routes.HomeController.index())
            .addingToSession(NINO -> matchedDetails.nino)
        )
    }
  }.map(_.removingFromSession(FailedUserMatching).withJourneyState(UserMatched).clearAllUserDetails)

}
