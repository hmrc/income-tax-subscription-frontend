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

package controllers.individual.controllist

import auth.individual.StatelessController
import config.AppConfig
import forms.individual.business.CannotSignUpThisYearForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import views.html.individual.eligibility.CannotSignUpThisYear

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CannotSignUpThisYearController @Inject()(val auditingService: AuditingService,
                                               val authService: AuthService,
                                               cannotSignUp: CannotSignUpThisYear)
                                              (implicit val appConfig: AppConfig,
                                               mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext) extends StatelessController {

  private val form: Form[YesNo] = CannotSignUpThisYearForm.cannotSignUpThisYearForm

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      Ok(cannotSignUp(form, routes.CannotSignUpThisYearController.submit))
  }


  def submit: Action[AnyContent] = Authenticated { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        hasErrors => BadRequest(view(form = hasErrors)), {
          case Yes => Redirect(controllers.individual.sps.routes.SPSHandoffController.redirectToSPS)
          case No => Redirect(controllers.individual.controllist.routes.DeclinedSignUpNextYearController.show)
        }
      )
  }

  private def view(form: Form[YesNo])(implicit request: Request[_]): Html = cannotSignUp(
    yesNoForm = form,
    postAction = routes.CannotSignUpThisYearController.submit
  )
}
