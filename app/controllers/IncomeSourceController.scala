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
import forms.IncomeSourceForm
import models.IncomeSourceModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object IncomeSourceController extends IncomeSourceController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait IncomeSourceController extends BaseController {

  val keystoreService: KeystoreService

  def view(incomeSourceForm: Form[IncomeSourceModel])(implicit request: Request[_]): Html =
    views.html.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.routes.IncomeSourceController.submitIncomeSource()
    )

  val showIncomeSource: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchIncomeSource() map {
        incomeSource => Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm.fill(incomeSource)))
      }
  }

  val submitIncomeSource: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      IncomeSourceForm.incomeSourceForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(incomeSourceForm = formWithErrors))),
        incomeSource =>
          keystoreService.saveIncomeSource(incomeSource) flatMap (
            _ =>
              incomeSource.source match {
                case IncomeSourceForm.option_business => business
                case IncomeSourceForm.option_property => property
                case IncomeSourceForm.option_both => both
              }
            )
      )
  }

  def business(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(controllers.business.routes.SoleTraderController.showSoleTrader()))

  def property(implicit request: Request[_]): Future[Result] = Future.successful(Redirect(controllers.property.routes.PropertyIncomeController.showPropertyIncome()))

  def both(implicit request: Request[_]): Future[Result] = Future.successful(NotImplemented)

}
