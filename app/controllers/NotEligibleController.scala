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
import forms.NotEligibleForm
import models.NotEligibleModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object NotEligibleController extends NotEligibleController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait NotEligibleController extends BaseController {

  val keystoreService: KeystoreService

  def view(notEligibleForm: Form[NotEligibleModel])(implicit request: Request[_]): Html =
    views.html.not_eligible(
      notEligibleForm = notEligibleForm,
      postAction = controllers.routes.NotEligibleController.submitNotEligible()
    )

  val showNotEligible: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchNotEligible.map {
        choice => Ok(view(NotEligibleForm.notEligibleForm.fill(choice)))
      }
  }

  val submitNotEligible: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      NotEligibleForm.notEligibleForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(notEligibleForm = formWithErrors))),
        choice => {
          keystoreService.saveNotEligible(choice).flatMap { _ =>
            choice.choice match {
              case NotEligibleForm.option_signup => signUp
              case NotEligibleForm.option_signout => signOut
            }
          }
        }
      )
  }

  def signUp(implicit request: Request[_]): Future[Result] = Future.successful(NotImplemented)

  def signOut(implicit request: Request[_]): Future[Result] = Future.successful(NotImplemented)

}
