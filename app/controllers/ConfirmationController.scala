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

import java.text.SimpleDateFormat
import auth.AuthorisedForIncomeTaxSA
import config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.Action
import scala.concurrent.Future

object ConfirmationController extends ConfirmationController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
}

trait ConfirmationController extends FrontendController with AuthorisedForIncomeTaxSA  {

  val showConfirmation = Authorised.async { implicit user => implicit request =>
    // The view accepts a dummy reference number & date, this will be replaced with
    // and actual value returned from DES once we have the service/connector implemented
		Future.successful(Ok(views.html.confirmation(
      submissionReference = "000-032407",
      submissionDate = new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2016")
    )))
  }

  val submitConfirmation = Authorised.async { implicit user => implicit request =>
    Future.successful(NotImplemented)
  }
}
