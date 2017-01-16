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

import auth.AuthorisedForIncomeTaxSA
import config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.KeystoreService
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

object SummaryController extends SummaryController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait SummaryController extends FrontendController with AuthorisedForIncomeTaxSA {

  val keystoreService: KeystoreService

  import services.CacheUtil._

  val showSummary = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchAll() map {
        case Some(cache) => val summary = cache.getSummary
        //TODO show page
      }
      Future.successful(NotImplemented)
  }

  val submitSummary = Authorised.async { implicit user =>
    implicit request =>
      Future.successful(Redirect(controllers.routes.ConfirmationController.showConfirmation()))
  }
}
