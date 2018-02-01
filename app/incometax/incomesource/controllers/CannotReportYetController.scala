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

package incometax.incomesource.controllers

import javax.inject.Inject

import core.audit.Logging
import core.auth.TaxYearDeferralController
import core.config.BaseControllerConfig
import core.models.DateModel
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.MatchTaxYearForm
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.twirl.api.HtmlFormat

import scala.concurrent.Future

class CannotReportYetController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val keystoreService: KeystoreService,
                                          val logging: Logging,
                                          val authService: AuthService
                                         ) extends TaxYearDeferralController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cache <- keystoreService.fetchAll()
        newIncomeSource = cache.getIncomeSourceType().get
        matchTaxYear = cache.getMatchTaxYear().map(_.matchTaxYear == MatchTaxYearForm.option_yes)
        dateModel = DateModel("6","4","2018")
      } yield
        Ok(generateTemplate(newIncomeSource, matchTaxYear, None, false))
  }

  def generateTemplate(incomeSourceType: IncomeSourceType,
                       matchToTaxYear: Option[Boolean] = None,
                       endBeforeNextTaxYear: Option[Boolean] = None,
                       isEditMode: Boolean)(implicit request: Request[_]): HtmlFormat.Appendable = {



    def generateCannotReportView = {
      incometax.incomesource.views.html.cannot_report_yet(
        postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
        backUrl(incomeSourceType, matchToTaxYear, isEditMode),
        //TODO update date model
        dateModel = DateModel("6","4","2018")
      )
    }

    def generateCannotReportPropertyView = {
      incometax.incomesource.views.html.cannot_report_property_yet(
        postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
        backUrl(incomeSourceType, matchToTaxYear, isEditMode),
        //TODO update date model
        dateModel = DateModel("6","4","2018")
      )
    }

    def generateCannotReportMisalignedView = {
      incometax.incomesource.views.html.cannot_report_yet_both_misaligned(
        postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
        backUrl(incomeSourceType, matchToTaxYear, isEditMode),
        //TODO update date model
        businessStartDate = DateModel("6","4","2018")
      )
    }

    (incomeSourceType, matchToTaxYear, endBeforeNextTaxYear) match {
      case (Property, _, _) => generateCannotReportView
      case (Business, Some(true), _) => generateCannotReportView
      case (Both, Some(false), _) => generateCannotReportView
      case (Both, _, Some(accountingPeriodCondition)) => if (accountingPeriodCondition) generateCannotReportView else generateCannotReportView
    }
  }


  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEditMode) Future.successful(Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show()))
      else
        for {
          cache <- keystoreService.fetchAll()
          newIncomeSource = cache.getIncomeSourceType().get
        } yield newIncomeSource match {
          case Business | Both =>
            Redirect(incometax.business.controllers.routes.BusinessAccountingMethodController.show())
          case Property =>
            Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show())
        }
  }

  def backUrl(incomeSourceType: IncomeSourceType, matchTaxYear: Option[Boolean], isEditMode: Boolean): String =
    (incomeSourceType, matchTaxYear) match {
      case (Property, _) if applicationConfig.newIncomeSourceFlowEnabled =>
        incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
      case (Property, _) =>
        incometax.incomesource.controllers.routes.IncomeSourceController.show().url
      case (Business | Both, Some(true)) =>
        incometax.business.controllers.routes.MatchTaxYearController.show(editMode = isEditMode).url
      case (Business | Both, Some(false)) =>
        incometax.business.controllers.routes.BusinessAccountingPeriodDateController.show(editMode = isEditMode).url
    }

}
