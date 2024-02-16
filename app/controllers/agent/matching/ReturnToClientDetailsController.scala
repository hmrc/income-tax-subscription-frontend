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

import auth.agent.StatelessController
import common.Constants.ITSASessionKeys
import config.AppConfig
import forms.agent.ReturnToClientDetailsForm
import models.ReturnToClientDetailsModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.matching.ReturnToClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class ReturnToClientDetailsController @Inject()(val auditingService: AuditingService,
                                                val authService: AuthService,
                                                val appConfig: AppConfig,
                                                val returnToClientDetails: ReturnToClientDetails)
                                               (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends StatelessController {


  def view(returnToClientDetailsForm: Form[ReturnToClientDetailsModel], clientName: String)(implicit request: Request[_]): Html = {
    returnToClientDetails(
      returnToClientDetailsViewForm = returnToClientDetailsForm,
      postAction = controllers.agent.matching.routes.ReturnToClientDetailsController.submit,
      clientName
    )
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(view(
        returnToClientDetailsForm = ReturnToClientDetailsForm.returnToClientDetailsForm,
        clientName = request.fetchClientName.getOrElse(
          throw new InternalServerException("[ReturnToClientDetailsController][show] - could not retrieve client name from session")
        )
      )
      )
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      val clientName = request.fetchClientName.getOrElse(
        throw new InternalServerException("[ReturnToClientDetailsController][submit] - could not retrieve client name from session")
      )
      ReturnToClientDetailsForm.returnToClientDetailsForm.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(
            view(
              returnToClientDetailsForm = formWithErrors,
              clientName
            )
          ),
        {
          case ReturnToClientDetailsModel.ContinueWithCurrentClient =>
            if (request.session.get(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY).contains("true")) {
              Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
            } else {
              Redirect(controllers.agent.matching.routes.HomeController.home)
            }
          case ReturnToClientDetailsModel.SignUpAnotherClient => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
        }
      )
  }
}
