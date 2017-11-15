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

import java.time.{Duration, LocalTime}
import javax.inject.Inject

import agent.auth.{IncomeTaxAgentUser, UserMatchingController}
import agent.services.AgentLockoutService
import core.config.BaseControllerConfig
import core.services.AuthService
import core.utils.Implicits._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.InternalServerException
import usermatching.models.{LockedOut, NotLockedOut}

import scala.concurrent.Future


class ClientDetailsLockoutController @Inject()(val baseConfig: BaseControllerConfig,
                                               val messagesApi: MessagesApi,
                                               val authService: AuthService,
                                               val lockoutService: AgentLockoutService
                                              ) extends UserMatchingController {

  private def handleLockOut(f: => Future[Result])(implicit user: IncomeTaxAgentUser, request: Request[_]) = {
    (lockoutService.getLockoutStatus(user.arn.get) flatMap {
      case Right(_: LockedOut) => f
      case Right(NotLockedOut) => Future.successful(Redirect(agent.controllers.matching.routes.ClientDetailsController.show()))
    }).recover { case e =>
      throw new InternalServerException("client details controller: " + e)
    }
  }

  private[controllers] def durationText(duration: Duration): String = {
    val dur = LocalTime.MIDNIGHT.plus(duration)

    def unitFormat(value: Int, text: String) = s"$value $text${if (value > 1) "s" else ""} "

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
        val duration = Duration.ofSeconds(baseConfig.applicationConfig.matchingLockOutSeconds)
        Ok(agent.views.html.client_details_lockout(postAction = agent.controllers.matching.routes.ClientDetailsLockoutController.submit(), durationText(duration)))
      }
  }

  lazy val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Redirect(core.controllers.routes.SignOutController.signOut())
  }

}