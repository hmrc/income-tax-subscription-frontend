/*
 * Copyright 2022 HM Revenue & Customs
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

package testonly.controllers.individual

import config.AppConfig
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import testonly.connectors.individual.{MatchingStubConnector, UserData}
import testonly.form.individual.UserToStubForm.userDetailsForm
import testonly.models.UserToStubModel
import testonly.views.html.individual.{ShowStubbedDetails, StubUser}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class MatchingStubController @Inject()(mcc: MessagesControllerComponents,
                                       matchingStubConnector: MatchingStubConnector,
                                       stubUser: StubUser,
                                       val showStubbedDetails: ShowStubbedDetails)
                                      (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {


  def view(clientToStubForm: Form[UserToStubModel])
          (implicit request: Request[_]): Html = {
    stubUser(
      clientToStubForm,
      postAction = routes.MatchingStubController.submit)
  }

  def show: Action[AnyContent] = Action { implicit request =>
    Ok(view(userDetailsForm.fill(UserData().toUserToStubModel)))
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    userDetailsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
      userDetails =>
        matchingStubConnector.newUser(userDetails) map {
          case true => Ok(showStubbedDetails(userDetails, routes.MatchingStubController.show))
          case _ => throw new InternalServerException("calls to matching-stub failed")
        }
    )
  }

}

// $COVERAGE-ON$
