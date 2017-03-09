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

import javax.inject.Inject

import config.BaseControllerConfig
import controllers.BaseController
import forms.RegisterNextAccountingPeriodForm
import models.RegisterNextAccountingPeriodModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future


class RegisterNextAccountingPeriodController @Inject()(val baseConfig: BaseControllerConfig,
                                                       val messagesApi: MessagesApi,
                                                       val keystoreService: KeystoreService
                                    ) extends BaseController {

  def view(registerNextAccountingPeriodForm: Form[RegisterNextAccountingPeriodModel])(implicit request: Request[_]): Html =
    views.html.business.register_next_accounting_period(
      registerNextAccountingPeriodForm = registerNextAccountingPeriodForm,
      postAction = controllers.business.routes.RegisterNextAccountingPeriodController.submit(),
      backUrl = controllers.business.routes.CurrentFinancialPeriodPriorController.show().url
    )

  val show: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      keystoreService.fetchRegisterNextAccountingPeriod().map { x =>
        Ok(view(RegisterNextAccountingPeriodForm.registerNextAccountingPeriodForm.fill(x)))
      }
  }

  val submit: Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      RegisterNextAccountingPeriodForm.registerNextAccountingPeriodForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        registerNextAccountingPeriod =>
          keystoreService.saveRegisterNextAccountingPeriod(registerNextAccountingPeriod) flatMap { _ =>
            registerNextAccountingPeriod.isRegisteringForNextPeriod match {
              case RegisterNextAccountingPeriodForm.option_yes => yes
              case RegisterNextAccountingPeriodForm.option_no => no
            }
          }
      )
  }

  def yes(implicit request: Request[_]): Future[Result] = Redirect(controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod())

  def no(implicit request: Request[_]): Future[Result] = NotImplemented //TODO: Needs to have 'Sign Out' functionality
}
