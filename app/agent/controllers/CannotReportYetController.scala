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

package agent.controllers

import javax.inject.Inject

import agent.services.CacheUtil._
import core.Constants.crystallisationTaxYearStart
import core.audit.Logging
import agent.auth.TaxYearDeferralController
import core.config.BaseControllerConfig
import core.models.DateModel
import core.services.AuthService
import agent.services.KeystoreService
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import incometax.util.AccountingPeriodUtil._
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.http.InternalServerException

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
        cache <- keystoreService.fetchAll() map (_.get)
        incomeSource = (cache.getIncomeSource() map (source => IncomeSourceType(source.source))).get
        accountingPeriod = cache.getAccountingPeriodDate()
      } yield {
        lazy val backUrl = incomeSource match {
          case Property =>
            agent.controllers.routes.IncomeSourceController.show().url
          case Business | Both =>
            agent.controllers.business.routes.BusinessAccountingPeriodDateController.show(editMode = isEditMode).url
        }

        val view = (incomeSource, accountingPeriod) match {
          case (Property, _) =>
            generateCannotReportView(crystallisationTaxYearStart, backUrl, isEditMode)
          case (Business, Some(businessDate)) =>
            generateCannotReportView(businessDate.endDate plusDays 1, backUrl, isEditMode)
          case (Both, Some(businessDate)) =>
            if (businessDate.endDate matches getCurrentTaxYearEndDate) {
              generateCannotReportView(crystallisationTaxYearStart, backUrl, isEditMode)
            } else if (businessDate.taxEndYear <= 2018) {
              generateCannotReportMisalignedView(businessDate.endDate plusDays 1, backUrl, isEditMode)
            } else {
              generateCanReportBusinessButNotPropertyView(backUrl, isEditMode)
            }
          case _ => throw new InternalServerException("The accounting period data was in an invalid state")
        }
        Ok(view)
      }
  }

  private def generateCannotReportView(dateModel: DateModel, backUrl: String, isEditMode: Boolean)(implicit request: Request[_]) =
    agent.views.html.client_cannot_report_yet(
      postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
      backUrl,
      dateModel = dateModel
    )

  private def generateCanReportBusinessButNotPropertyView(backUrl: String, isEditMode: Boolean)(implicit request: Request[_]) =
    agent.views.html.client_cannot_report_property_yet(
      postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
      backUrl,
      dateModel = crystallisationTaxYearStart //TODO - remove when page content is refactored
    )


  private def generateCannotReportMisalignedView(businessStartDate: DateModel, backUrl: String, isEditMode: Boolean)(implicit request: Request[_]) =
    agent.views.html.client_cannot_report_yet_both_misaligned(
      postAction = routes.CannotReportYetController.submit(editMode = isEditMode),
      backUrl,
      businessStartDate = businessStartDate
    )


  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEditMode) Future.successful(Redirect(agent.controllers.routes.CheckYourAnswersController.show()))
      else
        for {
          cache <- keystoreService.fetchAll() map (_.get)
          incomeSource = (cache.getIncomeSource() map (source => IncomeSourceType(source.source))).get
        } yield incomeSource match {
          case Business | Both =>
            Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
          case Property =>
            Redirect(agent.controllers.routes.OtherIncomeController.show())
        }
  }

}
