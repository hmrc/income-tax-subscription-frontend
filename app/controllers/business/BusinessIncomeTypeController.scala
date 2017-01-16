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
import forms.IncomeTypeForm
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.KeystoreService

import scala.concurrent.Future

object BusinessIncomeTypeController extends BusinessIncomeTypeController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait BusinessIncomeTypeController extends BaseController {

  val keystoreService: KeystoreService

  val showBusinessIncomeType = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchIncomeType() map {
        incomeType =>
          Ok(views.html.business.income_type(
            incomeTypeForm = IncomeTypeForm.incomeTypeForm.fill(incomeType),
            postAction = controllers.business.routes.BusinessIncomeTypeController.submitBusinessIncomeType()
          ))
      }
  }

  val submitBusinessIncomeType = Authorised.async { implicit user =>
    implicit request =>
      IncomeTypeForm.incomeTypeForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(NotImplemented)
        },
        incomeType => {
          keystoreService.saveIncomeType(incomeType) map (
            _ => Redirect(controllers.routes.ContactEmailController.showContactEmail()))
        }
      )
  }
}
