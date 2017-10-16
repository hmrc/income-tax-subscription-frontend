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

import javax.inject.{Inject, Singleton}

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import forms.AccountingMethodForm
import models.AccountingMethodModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService
                                                  ) extends SignUpController {

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.business.accounting_method(
      accountingMethodForm = accountingMethodForm,
      postAction = controllers.business.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingMethod() map {
        accountingMethod => Ok(view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(accountingMethodForm = formWithErrors, isEditMode = isEditMode))),
        accountingMethod => {
          keystoreService.saveAccountingMethod(accountingMethod) map (_ => isEditMode match {
            case true => Redirect(controllers.routes.CheckYourAnswersController.show())
            case _ => Redirect(controllers.routes.TermsController.showTerms())
          })
        }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.routes.CheckYourAnswersController.show().url
    else
      controllers.business.routes.BusinessAccountingPeriodDateController.show().url

}
