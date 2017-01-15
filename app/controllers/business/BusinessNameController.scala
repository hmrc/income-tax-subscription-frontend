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

import auth.AuthorisedForIncomeTaxSA
import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.BaseController
import forms.BusinessNameForm
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.data.Form
import services.KeystoreService

import scala.concurrent.Future

object BusinessNameController extends BusinessNameController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl

  override val keystoreService = KeystoreService
}

trait BusinessNameController extends BaseController {

  val keystoreService: KeystoreService

  val showBusinessName = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchBusinessName() map {
        businessNameModel =>
          Ok(views.html.business.business_name(
            businessNameForm = BusinessNameForm.businessNameForm.fill(businessNameModel),
            postAction = controllers.business.routes.BusinessNameController.submitBusinessName()
          ))
      }
  }

  val submitBusinessName = Authorised.async { implicit user =>
    implicit request =>
      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(NotImplemented)
        },
        businessName => {
          keystoreService.saveBusinessName(businessName) map (
            _ => Redirect(controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType()))
        }
      )
  }
}
