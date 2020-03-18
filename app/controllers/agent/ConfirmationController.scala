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

package controllers.agent

import agent.auth.PostSubmissionController
import agent.services.CacheUtil._
import core.config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.AuthService
import services.agent.KeystoreService
import views.html.agent.sign_up_complete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val authService: AuthService,
                                       val messagesApi: MessagesApi,
                                       keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
      val signOutAction = controllers.SignOutController.signOut(origin = routes.ConfirmationController.show())
      keystoreService.fetchAll() map (_.get.getSummary()) map {
        agentSummary => Ok(sign_up_complete(agentSummary, postAction, signOutAction))
      }
  }

}
