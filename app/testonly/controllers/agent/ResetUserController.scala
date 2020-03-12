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

package testonly.controllers.agent

import agent.auth.StatelessController
import controllers.agent.ITSASessionKeys
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResetUserController @Inject()(val authService: AuthService,
                                    val messagesApi: MessagesApi)
                                   (implicit val ec: ExecutionContext) extends StatelessController {

  val resetUser: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(
        Ok("User reset successfully")
          .removingFromSession(ITSASessionKeys.MTDITID)
      )
  }
}
