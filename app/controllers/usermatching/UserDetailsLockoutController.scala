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

package controllers.usermatching

import java.time.{Duration, LocalTime}

import auth.individual.{IncomeTaxSAUser, UserMatchingController}
import config.AppConfig
import javax.inject.Inject
import models.usermatching.{LockedOut, NotLockedOut}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{AuthService, UserLockoutService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}


class UserDetailsLockoutController @Inject()(val authService: AuthService, lockoutService: UserLockoutService)(implicit val ec: ExecutionContext,
                                             appConfig: AppConfig, mcc: MessagesControllerComponents) extends UserMatchingController {

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxSAUser, request: Request[_]): Future[Result] = {
    val bearerToken = implicitly[HeaderCarrier].userId.get.value
    lockoutService.getLockoutStatus(bearerToken) flatMap {
      case Right(_: LockedOut) => f
      case Right(NotLockedOut) => Future.successful(Redirect(controllers.usermatching.routes.UserDetailsController.show()))
      case Left(_) => throw new InternalServerException("[UserDetailsLockoutController][handleLockOut] failure response returned from lockout service")
    }
  }

  private[controllers] def durationText(duration: Duration)(implicit request: Request[_]): String = {
    val dur = LocalTime.MIDNIGHT.plus(duration)

    def unitFormat(value: Int, text: String) = {
      val messageKey = s"base.$text${if (value > 1) "s" else ""}"
      s"$value ${Messages(messageKey)} "
    }

    val h = dur.getHour
    lazy val hs = unitFormat(h, "hour")
    val m = dur.getMinute
    lazy val ms = unitFormat(m, "minute")
    val s = dur.getSecond
    lazy val ss = unitFormat(s, "second")

    s"${if (h > 0) hs else ""}${if (m > 0) ms else ""}${if (s > 0) ss else ""}".trim
  }

  lazy val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleLockOut {
        val duration = Duration.ofSeconds(appConfig.matchingLockOutSeconds)
        Future.successful(Ok(views.html.individual.usermatching.user_details_lockout(durationText(duration))))
      }
  }

}
