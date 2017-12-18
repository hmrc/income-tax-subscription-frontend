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

package incometax.business.controllers

import javax.inject.{Inject, Singleton}

import core.auth.{Registration, SignUpController}
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import core.utils.Implicits._
import incometax.business.forms.AccountingPeriodDateForm
import incometax.business.models.AccountingPeriodModel
import incometax.business.models.enums._
import incometax.util.AccountingPeriodUtil
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessAccountingPeriodDateController @Inject()(val baseConfig: BaseControllerConfig,
                                                       val messagesApi: MessagesApi,
                                                       val keystoreService: KeystoreService,
                                                       val authService: AuthService
                                                      ) extends SignUpController {

  def view(form: Form[AccountingPeriodModel], backUrl: String, isEditMode: Boolean, editMatch: Boolean, viewType: AccountingPeriodViewType)(implicit request: Request[_]): Html =
    incometax.business.views.html.accounting_period_date(
      form,
      incometax.business.controllers.routes.BusinessAccountingPeriodDateController.submit(editMode = isEditMode, editMatch = editMatch),
      viewType,
      isEditMode,
      backUrl
    )

  def show(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        accountingPeriod <- keystoreService.fetchAccountingPeriodDate()
        viewType <- whichView
      } yield
        Ok(view(
          AccountingPeriodDateForm.accountingPeriodDateForm.fill(accountingPeriod),
          backUrl = backUrl(isEditMode, editMatch),
          isEditMode = isEditMode,
          editMatch = editMatch,
          viewType = viewType
        ))
  }

  def submit(isEditMode: Boolean, editMatch: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      whichView.flatMap {
        viewType =>
          AccountingPeriodDateForm.accountingPeriodDateForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(
              form = formWithErrors,
              backUrl = backUrl(isEditMode, editMatch),
              isEditMode = isEditMode,
              editMatch = editMatch,
              viewType = viewType
            ))),
            accountingPeriod => {
              lazy val linearRedirect = Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
              lazy val checkYourAnswersRedirect = Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
              lazy val termsRedirect = Redirect(incometax.subscription.controllers.routes.TermsController.showTerms(editMode = true))

              def saveAndUpdate(taxYearChanged: Boolean): Future[Result] =
                if (taxYearChanged) keystoreService.saveTerms(terms = false) flatMap { _ =>
                  Future.successful(if (isEditMode) termsRedirect else linearRedirect)
                }
                else Future.successful(if (isEditMode) checkYourAnswersRedirect else linearRedirect)

              {
                for {
                  optOldAccountingPeriodDates <- keystoreService.fetchAccountingPeriodDate()
                  _ <- keystoreService.saveAccountingPeriodDate(accountingPeriod)
                } yield optOldAccountingPeriodDates match {
                  case Some(oldAccountingPeriodDates) =>
                    val oldEndYear = AccountingPeriodUtil.getTaxEndYear(oldAccountingPeriodDates)
                    val newEndYear = AccountingPeriodUtil.getTaxEndYear(accountingPeriod)
                    saveAndUpdate(oldEndYear != newEndYear)
                  case _ => saveAndUpdate(taxYearChanged = false) // can only happen on linear journey so we don't want to update terms
                }
              } flatMap identity
            }
          )
      }
    }
  }

  def whichView(implicit request: Request[_]): Future[AccountingPeriodViewType] = {
    if (request.isInState(Registration)) RegistrationAccountingPeriodView
    else {
      SignUpAccountingPeriodView
    }
  }

  def backUrl(isEditMode: Boolean, editMatch: Boolean)(implicit request: Request[_]): String =
    if (isEditMode) {
      if (editMatch) incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url
      else incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
    else if (request.isInState(Registration))
      incometax.business.controllers.routes.BusinessStartDateController.show().url
    else
      incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url

}
