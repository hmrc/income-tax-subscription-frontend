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

package controllers.property

import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.BaseController
import forms.PropertyIncomeForm
import models.PropertyIncomeModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object PropertyIncomeController extends PropertyIncomeController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait PropertyIncomeController extends BaseController {

  val keystoreService: KeystoreService

  def view(propertyIncomeForm: Form[PropertyIncomeModel])(implicit request: Request[_]): Html =
    views.html.property.property_income(
      propertyIncomeForm = propertyIncomeForm,
      postAction = controllers.property.routes.PropertyIncomeController.submitPropertyIncome()
    )

  val showPropertyIncome: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchPropertyIncome() map {
        incomeType => Ok(view(propertyIncomeForm = PropertyIncomeForm.propertyIncomeForm.fill(incomeType)))
      }
  }

  val submitPropertyIncome: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      PropertyIncomeForm.propertyIncomeForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(propertyIncomeForm = formWithErrors))),
        propertyIncome => {
          keystoreService.savePropertyIncome(propertyIncome) flatMap { _ =>
            propertyIncome.incomeValue match {
              case PropertyIncomeForm.option_LT10k => notEligible
              case PropertyIncomeForm.option_GE10k => eligible
            }
          }
        }
      )
  }

  def eligible(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(controllers.routes.EligibleController.showEligible()))

  def notEligible(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(controllers.routes.NotEligibleController.showNotEligible()))
}
