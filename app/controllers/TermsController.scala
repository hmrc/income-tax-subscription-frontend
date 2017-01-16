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
import forms.TermForm
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.KeystoreService

import scala.concurrent.Future

object TermsController extends TermsController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait TermsController extends BaseController {

  val keystoreService: KeystoreService

  val showTerms = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchTerms() map {
        terms =>
          Ok(views.html.terms(
            termsForm = TermForm.termForm.fill(terms),
            postAction = controllers.routes.TermsController.submitTerms()
          ))
      }
  }

  val submitTerms = Authorised.async { implicit user =>
    implicit request =>
      TermForm.termForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(NotImplemented)
        },
        terms => {
          keystoreService.saveTerms(terms) map (
            _ => Redirect(controllers.routes.SummaryController.showSummary()))
        }
      )
  }
}