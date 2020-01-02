/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.subscription

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.models.{No, Yes, YesNo}
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.business.models.MatchTaxYearModel
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import incometax.util.AccountingPeriodUtil._
import javax.inject.{Inject, Singleton}
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
      postAction = controllers.individual.subscription.routes.TermsController.submit(),
      taxEndYear = taxEndYear,
      backUrl
    )

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cacheMap <- keystoreService.fetchAll()
        incomeSource = cacheMap.getIncomeSourceType().get
        backUrl = getBackUrl(editMode, incomeSource, cacheMap.getOtherIncome().get, cacheMap.getMatchTaxYear().exists(_.matchTaxYear == Yes))
      } yield
        (incomeSource, cacheMap.getMatchTaxYear(), cacheMap.getEnteredAccountingPeriodDate()) match {
          case (Property, _, _) =>
            Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
          case (_, Some(MatchTaxYearModel(Yes)), _) =>
            Ok(view(backUrl = backUrl, taxEndYear = getCurrentTaxEndYear))
          case (_, _, Some(date)) =>
            Ok(view(backUrl = backUrl, taxEndYear = date.taxEndYear))
          case _ =>
            Redirect(controllers.individual.business.routes.BusinessAccountingPeriodDateController.show(editMode = editMode, editMatch = editMode))
        }

  }

  def submit(isEditMode: Boolean = false): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.saveTerms(terms = true) map (
        _ => Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
  }

  def getBackUrl(editMode: Boolean, incomeSource: IncomeSourceType, otherIncome: YesNo, matchTaxYear: Boolean)(implicit request: Request[_]): String =
    if (editMode && matchTaxYear)
      controllers.individual.business.routes.MatchTaxYearController.show(editMode = true).url
    else if (editMode)
      controllers.individual.business.routes.BusinessAccountingPeriodDateController.show(editMode = true).url
    else
      incomeSource match {
        case (Business | Both) =>
          controllers.individual.business.routes.BusinessAccountingMethodController.show().url
        case Property =>
          otherIncome match {
            case Yes =>
              controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url
            case No =>
              controllers.individual.incomesource.routes.OtherIncomeController.show().url
          }
      }
}

