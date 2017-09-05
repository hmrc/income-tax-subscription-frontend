/*
 * Copyright 2017 HM Revenue & Customs
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

package testonly.controllers

import javax.inject.{Inject, Singleton}

import config.{AppConfig, BaseControllerConfig}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.twirl.api.Html
import testonly.connectors.{MatchingStubConnector, UserData}
import testonly.forms.UserToStubForm
import testonly.models.UserToStubModel
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.InternalServerException
import utils.Implicits._

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class MatchingStubController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       matchingStubConnector: MatchingStubConnector
                                      ) extends FrontendController with I18nSupport {

  implicit lazy val appConfig: AppConfig = baseConfig.applicationConfig

  def view(clientToStubForm: Form[UserToStubModel])(implicit request: Request[_]): Html =
    testonly.views.html.stub_user(
      clientToStubForm,
      routes.MatchingStubController.stubUser()
    )

  def show = Action.async { implicit request =>
    Ok(view(UserToStubForm.userToStubForm.form.fill(UserData().toUserToStubModel)))
  }

  def stubUser = Action.async { implicit request =>
    UserToStubForm.userToStubForm.bindFromRequest.fold(
      formWithErrors => BadRequest(view(formWithErrors)),
      userDetails =>
        matchingStubConnector.newUser(userDetails) map {
          case true => Ok(testonly.views.html.show_stubbed_details(userDetails))
          case _ => throw new InternalServerException("calls to matching-stub failed")
        }
    )
  }

}

// $COVERAGE-ON$
