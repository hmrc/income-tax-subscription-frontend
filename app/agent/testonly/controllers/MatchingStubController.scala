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

package agent.testonly.controllers

import javax.inject.{Inject, Singleton}

import core.config.{AppConfig, BaseControllerConfig}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.twirl.api.Html
import agent.testonly.connectors.{MatchingStubConnector, UserData}
import agent.testonly.forms.ClientToStubForm
import agent.testonly.models.ClientToStubModel
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import core.utils.Implicits._
import uk.gov.hmrc.http.InternalServerException

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class MatchingStubController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       matchingStubConnector: MatchingStubConnector
                                      ) extends FrontendController with I18nSupport {

  implicit lazy val appConfig: AppConfig = baseConfig.applicationConfig

  def view(clientToStubForm: Form[ClientToStubModel])(implicit request: Request[_]): Html =
    agent.testonly.views.html.stub_client(
      clientToStubForm,
      agent.testonly.controllers.routes.MatchingStubController.submit()
    )

  def show = Action.async { implicit request =>
    Ok(view(ClientToStubForm.clientToStubForm.form.fill(UserData().toClientToStubModel)))
  }

  def submit = Action.async { implicit request =>
    ClientToStubForm.clientToStubForm.bindFromRequest.fold(
      formWithErrors => BadRequest(view(formWithErrors)),
      clientDetails =>
        matchingStubConnector.newUser(clientDetails) map {
          case true => Ok(agent.testonly.views.html.show_stubbed_details(clientDetails))
          case _ => throw new InternalServerException("calls to matching-stub failed")
        }
    )
  }

}

// $COVERAGE-ON$
