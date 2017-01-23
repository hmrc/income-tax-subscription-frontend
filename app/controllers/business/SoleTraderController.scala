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
import forms.SoleTraderForm
import models.SoleTraderModel
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService

import scala.concurrent.Future

object SoleTraderController extends SoleTraderController {
  override lazy val applicationConfig = FrontendAppConfig
  override lazy val authConnector = FrontendAuthConnector
  override lazy val postSignInRedirectUrl = FrontendAppConfig.ggSignInContinueUrl
  override val keystoreService = KeystoreService
}

trait SoleTraderController extends BaseController {

  val keystoreService: KeystoreService

  def view(soleTraderForm: Form[SoleTraderModel])(implicit request: Request[_]): Html =
    views.html.business.sole_trader(
      soleTraderForm = soleTraderForm,
      postAction = controllers.business.routes.SoleTraderController.submitSoleTrader()
    )


  val showSoleTrader: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchSoleTrader() map {
        soleTrader => Ok(view(SoleTraderForm.soleTraderForm.fill(soleTrader)))
      }
  }

  val submitSoleTrader: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      SoleTraderForm.soleTraderForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        soleTrader =>
          keystoreService.saveSoleTrader(soleTrader) flatMap { _ =>
            soleTrader.isSoleTrader match {
              case SoleTraderForm.option_yes => yes
              case SoleTraderForm.option_no => no
            }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] =
    Future.successful(Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod()))

  def no(implicit request: Request[_]): Future[Result] =
    Future.successful(NotImplemented)

}
