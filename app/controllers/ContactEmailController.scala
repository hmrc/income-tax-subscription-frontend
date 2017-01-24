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

package controllers

import config.{FrontendAppConfig, FrontendAuthConnector}
import forms.{EmailForm, IncomeSourceForm}
import models.EmailModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object ContactEmailController extends ContactEmailController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait ContactEmailController extends BaseController {

  val keystoreService: KeystoreService

  def view(contactEmailForm: Form[EmailModel], backUrl: String)(implicit request: Request[_]): Html =
    views.html.contact_email(
      contactEmailForm = contactEmailForm,
      postAction = controllers.routes.ContactEmailController.submitContactEmail(),
      backUrl = backUrl
    )

  val showContactEmail: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        contactEmail <- keystoreService.fetchContactEmail()
        backUrl <- backUrl
      } yield Ok(view(contactEmailForm = EmailForm.emailForm.fill(contactEmail), backUrl = backUrl))
  }

  val submitContactEmail: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      EmailForm.emailForm.bindFromRequest.fold(
        formWithErrors => backUrl.map(backUrl => BadRequest(view(contactEmailForm = formWithErrors, backUrl = backUrl))),
        contactEmail => {
          keystoreService.saveContactEmail(contactEmail) map (
            _ => Redirect(controllers.routes.TermsController.showTerms()))
        }
      )
  }

  def backUrl(implicit request: Request[_]): Future[String] =
    keystoreService.fetchIncomeSource() map {
      case Some(source) => source.source match {
        case IncomeSourceForm.option_business | IncomeSourceForm.option_both =>
          controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url
        case _ =>
          controllers.routes.EligibleController.showEligible().url
      }
    }

}