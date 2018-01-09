/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.unauthorisedagent.controllers

import javax.inject.{Inject, Singleton}

import core.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.services.AuthService
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import usermatching.userjourneys.ConfirmAgentSubscription

import scala.concurrent.Future

@Singleton
class ConfirmAgentController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val authService: AuthService
                                      ) extends AuthenticatedController[ConfirmAgentSubscription.type] {

  def getAgentName: Future[String] = Future.successful("") //TODO

  def view(form: Form[ConfirmAgentModel], agentName: String)(implicit request: Request[_]): Html =
    incometax.unauthorisedagent.views.html.confirm_agent(
      confirmAgentForm = form,
      agentName = agentName,
      postAction = incometax.unauthorisedagent.controllers.routes.ConfirmAgentController.submit()
    )

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getAgentName.map(agentName => Ok(view(ConfirmAgentForm.confirmAgentForm, agentName = agentName)))
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      ConfirmAgentForm.confirmAgentForm.bindFromRequest.fold(
        formWithErrors => getAgentName.map(agentName => BadRequest(view(formWithErrors, agentName = agentName))),
        isAgent => isAgent.choice match {
          case ConfirmAgentForm.option_yes =>
            Future.successful(NotImplemented) //TODO
          case ConfirmAgentForm.option_no =>
            Future.successful(NotImplemented) //TODO
        }
      )
  }


}
