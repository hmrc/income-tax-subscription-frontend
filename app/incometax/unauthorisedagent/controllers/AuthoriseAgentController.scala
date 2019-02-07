/*
 * Copyright 2019 HM Revenue & Customs
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

import core.auth.JourneyState._
import core.ITSASessionKeys._
import core.auth.AuthenticatedController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import incometax.subscription.services.SubscriptionOrchestrationService
import incometax.unauthorisedagent.forms.ConfirmAgentForm
import incometax.unauthorisedagent.models.ConfirmAgentModel
import incometax.unauthorisedagent.services.SubscriptionStoreRetrievalService
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import usermatching.userjourneys.ConfirmAgentSubscription

import scala.concurrent.Future

@Singleton
class AuthoriseAgentController @Inject()(val baseConfig: BaseControllerConfig,
                                         val messagesApi: MessagesApi,
                                         val authService: AuthService
                                        ) extends AuthenticatedController[ConfirmAgentSubscription.type] {

  lazy val goToPreferences = Redirect(digitalcontact.controllers.routes.PreferencesController.checkPreferences())

  def view(form: Form[ConfirmAgentModel])(implicit request: Request[_]): Html =
    incometax.unauthorisedagent.views.html.authorise_agent(
      authoriseAgentForm = form,
      postAction = routes.AuthoriseAgentController.submit()
    )

  def show(): Action[AnyContent] = Authenticated { implicit req =>
    implicit user =>
      Ok(view(ConfirmAgentForm.confirmAgentForm))
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit req =>
    implicit user =>
      ConfirmAgentForm.confirmAgentForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        authoriseAgent => authoriseAgent.choice match {
          case ConfirmAgentForm.option_yes =>
            Future.successful(goToPreferences.confirmAgent)
          case ConfirmAgentForm.option_no =>
            Future.successful(Redirect(routes.AgentNotAuthorisedController.show()))
        }
      )
  }

}
