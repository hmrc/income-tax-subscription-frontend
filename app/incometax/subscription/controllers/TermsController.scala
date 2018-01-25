/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.subscription.controllers

import javax.inject.{Inject, Singleton}

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.MatchTaxYearForm
import incometax.business.models.MatchTaxYearModel
import incometax.incomesource.forms.IncomeSourceForm.option_property
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.IncomeSourceModel
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import incometax.util.AccountingPeriodUtil._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

@Singleton
class TermsController @Inject()(val baseConfig: BaseControllerConfig,
                                val messagesApi: MessagesApi,
                                val keystoreService: KeystoreService,
                                val authService: AuthService
                               ) extends SignUpController {

  def view(backUrl: String, taxEndYear: Int)(implicit request: Request[_]): Html =
    incometax.subscription.views.html.terms(
      postAction = incometax.subscription.controllers.routes.TermsController.submit(),
      taxEndYear = taxEndYear,
      backUrl
    )

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (applicationConfig.newIncomeSourceFlowEnabled) {
        for {
          cacheMap <- keystoreService.fetchAll()
          incomeSource = cacheMap.getNewIncomeSource().get.getIncomeSourceType.right.get
          backUrl = getBackUrl(editMode, incomeSource, cacheMap.getOtherIncome().get.choice, cacheMap.getMatchTaxYear().fold(false)(_.matchTaxYear == MatchTaxYearForm.option_yes))
        } yield
          (incomeSource, cacheMap.getMatchTaxYear(), cacheMap.getAccountingPeriodDate()) match {
            case (Property, _, _) =>
              Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
            case (_, Some(MatchTaxYearModel(matchTaxYear)), _) if matchTaxYear == MatchTaxYearForm.option_yes =>
              Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
            case (_, _, Some(date)) =>
              Ok(view(backUrl = backUrl, taxEndYear = date.taxEndYear))
            case _ =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = editMode, editMatch = editMode))
          }
      }
      else {
        for {
          cacheMap <- keystoreService.fetchAll()
          incomeSource = cacheMap.getIncomeSource().get
          backUrl = getBackUrl(editMode, IncomeSourceType(incomeSource.source), cacheMap.getOtherIncome().get.choice, cacheMap.getMatchTaxYear().fold(false)(_.matchTaxYear == MatchTaxYearForm.option_yes))
        } yield
          (incomeSource, cacheMap.getMatchTaxYear(), cacheMap.getAccountingPeriodDate()) match {
            case (IncomeSourceModel(source), _, _) if source == option_property =>
              Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
            case (_, Some(MatchTaxYearModel(matchTaxYear)), _) if matchTaxYear == MatchTaxYearForm.option_yes =>
              Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
            case (_, _, Some(date)) =>
              Ok(view(backUrl = backUrl, taxEndYear = date.taxEndYear))
            case _ =>
              Redirect(incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = editMode, editMatch = editMode))
          }
      }
  }

  def submit(isEditMode: Boolean = false): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.saveTerms(terms = true) map (
        _ => Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
  }

  def getBackUrl(editMode: Boolean, incomeSource: IncomeSourceType, otherIncome: String, matchTaxYear: Boolean)(implicit request: Request[_]): String =
    if (editMode && matchTaxYear)
      incometax.business.controllers.routes.MatchTaxYearController.show(editMode = true).url
    else if (editMode)
      incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
    else
      incomeSource match {
        case (Business | Both) =>
          incometax.business.controllers.routes.BusinessAccountingMethodController.show().url
        case Property =>
          otherIncome match {
            case OtherIncomeForm.option_yes =>
              incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url
            case OtherIncomeForm.option_no =>
              incometax.incomesource.controllers.routes.OtherIncomeController.show().url
          }
      }
}

