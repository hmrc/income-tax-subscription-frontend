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

package controllers.business

import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.BaseController
import forms.AccountingPeriodForm
import models.AccountingPeriodModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object BusinessAccountingPeriodController extends BusinessAccountingPeriodController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl

  override val keystoreService = KeystoreService
}

trait BusinessAccountingPeriodController extends BaseController {

  val keystoreService: KeystoreService

  def view(form: Form[AccountingPeriodModel])(implicit request: Request[_]): Html =
    views.html.business.accounting_period(
      form,
      controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod()
    )

  val showAccountingPeriod: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAccountingPeriod() map {
        accountingPeriod =>
          Ok(view(
            AccountingPeriodForm.accountingPeriodForm.fill(accountingPeriod)
          ))
      }
  }

  val submitAccountingPeriod: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      AccountingPeriodForm.accountingPeriodForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        accountingPeriod => {
          keystoreService.saveAccountingPeriod(accountingPeriod) map (
            _ => Redirect(controllers.business.routes.BusinessNameController.showBusinessName()))
        }
      )
  }
}
