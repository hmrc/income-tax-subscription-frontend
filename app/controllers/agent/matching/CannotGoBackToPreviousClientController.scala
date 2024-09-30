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
import forms.agent.CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClientForm
import models.CannotGoBack
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.agent.matching.CannotGoBackToPreviousClient

import javax.inject.{Inject, Singleton}

@Singleton
class CannotGoBackToPreviousClientController @Inject()(view: CannotGoBackToPreviousClient,
                                                       identify: IdentifierAction,
                                                       appConfig: AppConfig)
                                                      (implicit cc: MessagesControllerComponents) extends SignUpBaseController {

  val show: Action[AnyContent] = identify { implicit request =>
    Ok(view(
      cannotGoBackToPreviousClientForm = cannotGoBackToPreviousClientForm,
      postAction = routes.CannotGoBackToPreviousClientController.submit
    ))
  }

  val submit: Action[AnyContent] = identify { implicit request =>
    cannotGoBackToPreviousClientForm.bindFromRequest().fold(
      formWithErrors => BadRequest(view(
        cannotGoBackToPreviousClientForm = formWithErrors,
        postAction = routes.CannotGoBackToPreviousClientController.submit
      )),
      {
        case CannotGoBack.AgentServiceAccount =>
          Redirect(appConfig.agentServicesAccountHomeUrl)
        case CannotGoBack.ReenterClientDetails | CannotGoBack.SignUpAnotherClient =>
          Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
      }
    )
  }

}
