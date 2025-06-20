/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import models.requests.agent.IdentifierRequest
import models.usermatching.{LockedOut, NotLockedOut}
import play.api.i18n.Messages
import play.api.mvc._
import services.UserLockoutService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.matching.ClientDetailsLockout

import java.time.{Duration, LocalTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsLockoutController @Inject()(identify: IdentifierAction,
                                               lockoutService: UserLockoutService,
                                               clientDetailsLockOut: ClientDetailsLockout)
                                              (implicit val ec: ExecutionContext,
                                               val appConfig: AppConfig,
                                               mcc: MessagesControllerComponents) extends SignUpBaseController {

  private def handleLockOut(f: => Future[Result])(implicit request: IdentifierRequest[_]): Future[Result] = {
    lockoutService.getLockoutStatus(request.arn) flatMap {
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

  lazy val show: Action[AnyContent] = identify async { implicit request =>
    handleLockOut {
      val duration = Duration.ofSeconds(appConfig.matchingLockOutSeconds)
      Future.successful(Ok(clientDetailsLockOut(durationText(duration))))
    }
  }

}
