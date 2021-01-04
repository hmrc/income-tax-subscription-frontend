/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import testonly.connectors.agent.{MatchingStubConnector, UserData}
import testonly.form.agent.ClientToStubForm
import testonly.models.agent.ClientToStubModel
import testonly.views.html.agent.{show_stubbed_details, stub_client}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utilities.Implicits._

import scala.concurrent.ExecutionContext


//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class MatchingStubController @Inject()(mcc: MessagesControllerComponents, matchingStubConnector: MatchingStubConnector)
                                      (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {


  def view(clientToStubForm: Form[ClientToStubModel])(implicit request: Request[_]): Html =
    stub_client(
      clientToStubForm,
      testonly.controllers.agent.routes.MatchingStubController.submit()
    )

  def show: Action[AnyContent] = Action.async { implicit request =>
    Ok(view(ClientToStubForm.clientToStubForm.form.fill(UserData().toClientToStubModel)))
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    ClientToStubForm.clientToStubForm.bindFromRequest.fold(
      formWithErrors => BadRequest(view(formWithErrors)),
      clientDetails =>
        matchingStubConnector.newUser(clientDetails) map {
          case true => Ok(show_stubbed_details(clientDetails))
          case _ => throw new InternalServerException("calls to matching-stub failed")
        }
    )
  }

}

// $COVERAGE-ON$
