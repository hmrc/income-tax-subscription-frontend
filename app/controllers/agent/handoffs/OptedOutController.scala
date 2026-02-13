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

package controllers.agent.handoffs

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.agent.handoffs.OptedOut

import javax.inject.{Inject, Singleton}

@Singleton
class OptedOutController @Inject()(
                                    view: OptedOut,
                                    identity: IdentifierAction,
                                    appConfig: AppConfig
                                  )(implicit mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = identity { implicit request =>
    Ok(view(
      changeAction = controllers.agent.handoffs.routes.OptedOutController.change,
      postAction = controllers.agent.handoffs.routes.OptedOutController.submit
    ))
  }

  def change: Action[AnyContent] = identity { implicit request =>
    Redirect(appConfig.getVAndCUrl)
  }

  def submit: Action[AnyContent] = identity { implicit request =>
    Redirect(controllers.agent.routes.AddAnotherClientController.addAnother().url)
  }
}
