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

import java.time.{Duration, LocalTime}

import agent.auth.{IncomeTaxAgentUser, UserMatchingController}
import core.config.BaseControllerConfig
import core.utils.Implicits._
import javax.inject.Inject
import models.usermatching.{LockedOut, NotLockedOut}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{AuthService, UserLockoutService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.{ExecutionContext, Future}


class ClientDetailsLockoutController @Inject()(val baseConfig: BaseControllerConfig,
                                               val messagesApi: MessagesApi,
                                               val authService: AuthService,
                                               val lockoutService: UserLockoutService
                                              )(implicit val ec: ExecutionContext) extends UserMatchingController {

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]): Future[Result] = {
    lockoutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(_: LockedOut) => f
      case Right(NotLockedOut) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsController.show()))
      case Left(_) => throw new InternalServerException("[ClientDetailsLockoutController][handleLockOut] lockout status failure")
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
        val duration = Duration.ofSeconds(baseConfig.appConfig.matchingLockOutSeconds)
        Ok(views.html.agent.client_details_lockout(durationText(duration)))
      }
  }

}
