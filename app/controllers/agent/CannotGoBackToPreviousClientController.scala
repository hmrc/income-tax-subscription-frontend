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

package controllers.agent

import auth.agent.StatelessController
import config.AppConfig
import forms.agent.CannotGoBackToPreviousClientForm
import forms.agent.CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm
import models.CannotGoBack
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import views.html.agent.CannotGoBackToPreviousClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class CannotGoBackToPreviousClientController @Inject()(val auditingService: AuditingService,
                                                       val authService: AuthService,
                                                       val appConfig: AppConfig,
                                                       val cannotGoBackToPreviousClient: CannotGoBackToPreviousClient
                                                      )
                                                      (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends StatelessController {


  def view(cannotGoBackToPreviousClientForm: Form[CannotGoBack])(implicit request: Request[_]): Html = {
    cannotGoBackToPreviousClient(
      cannotGoBackToPreviousClientForm = cannotGoBackTpPreviousClientForm,
      postAction = controllers.agent.routes.CannotGoBackToPreviousClientController.submit
    )
  }

  def show(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
        Ok(view(cannotGoBackToPreviousClientForm = CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm))
  }




  def submit(): Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
        CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(view(cannotGoBackToPreviousClientForm = formWithErrors)),
          {
            case CannotGoBack.AgentServiceAccount => Redirect(appConfig.agentServicesAccountHomeUrl)
            case CannotGoBack.ReenterClientDetails => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
            case CannotGoBack.SignUpAnotherClient => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
          }
        )
    }
}
