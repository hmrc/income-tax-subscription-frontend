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


import auth.agent.PostSubmissionController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuthService, KeystoreService}
import utilities.CacheUtil._
import views.html.agent.sign_up_complete

import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(val authService: AuthService, keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      val postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
      val signOutAction = controllers.SignOutController.signOut(origin = routes.ConfirmationController.show())
      keystoreService.fetchAll() map { cacheMap =>
        Ok(sign_up_complete(cacheMap.getAgentSummary(), postAction, signOutAction))
      }
  }
}
