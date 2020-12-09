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

package controllers.agent

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, PropertyNextTaxYear, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import forms.agent.IncomeSourceForm
import javax.inject.{Inject, Singleton}
import models.AgentSummary
import models.common.IncomeSourceModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil.CacheMapUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService,
                                      incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends AuthenticatedController with FeatureSwitching {

  def view(incomeSourceForm: Form[IncomeSourceModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.agent.income_source(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.agent.routes.IncomeSourceController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl,
      foreignProperty = isEnabled(ForeignProperty)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchIncomeSource() map { incomeSource =>
        Ok(view(incomeSourceForm = IncomeSourceForm.incomeSourceForm
          .fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      IncomeSourceForm.incomeSourceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(incomeSourceForm = formWithErrors, isEditMode = isEditMode))),
        incomeSource => {
          if (!isEditMode) {
            linearJourney(incomeSource)
          } else {
           editJourney(incomeSource)
          }
        }
      )
  }

  private[controllers] def editJourney(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      cacheMap <- subscriptionDetailsService.saveIncomeSource(incomeSource)
      summaryModel <- getSummaryModel(cacheMap)
    } yield {
      if (isEnabled(ReleaseFour) && isEnabled(PropertyNextTaxYear)) {
        if (cacheMap.getSelectedTaxYear.isDefined) {
          incomeSource match {
            case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = true, ignoreSelectedTaxYear = true) =>
              Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
            case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(true) =>
              Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
            case IncomeSourceModel(_, _, true) if !summaryModel.foreignPropertyComplete =>
              Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
            case _ =>
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
          }
        } else {
          Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show())
        }

      } else if (isEnabled(ReleaseFour)) {

        incomeSource match {
          case IncomeSourceModel(true, false, false) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = true, ignoreSelectedTaxYear = false) =>
            Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show())
          case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = true, ignoreSelectedTaxYear = true) =>
            Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
          case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(true) =>
            Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
          case IncomeSourceModel(_, _, true) if !summaryModel.foreignPropertyComplete =>
            Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
          case _ =>
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        }

      } else {
        incomeSource match {
          case IncomeSourceModel(true, false, false) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = false, ignoreSelectedTaxYear = false) =>
            Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show())
          case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = false, ignoreSelectedTaxYear = true) =>
            Redirect(controllers.agent.business.routes.BusinessNameController.show())
          case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(false) =>
            Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
          case _ =>
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        }
      }
    }
  }

  private def getSummaryModel(cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[AgentSummary] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
    } yield {
      if (isEnabled(ReleaseFour)) {
        cacheMap.getAgentSummary(businesses, businessAccountingMethod)
      } else {
        cacheMap.getAgentSummary()
      }
    }
  }

  private def linearJourney(incomeSource: IncomeSourceModel)(implicit request: Request[_]): Future[Result] = {
    subscriptionDetailsService.saveIncomeSource(incomeSource) map { _ =>
      incomeSource match {
        case IncomeSourceModel(_, _, _) if isEnabled(PropertyNextTaxYear) =>
          Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show())
        case IncomeSourceModel(true, false, false) =>
          Redirect(controllers.agent.business.routes.WhatYearToSignUpController.show())
        case IncomeSourceModel(true, _, _) =>
          if (isEnabled(ReleaseFour)) Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
          else Redirect(controllers.agent.business.routes.BusinessNameController.show())
        case IncomeSourceModel(_, true, _) =>
          if (isEnabled(ReleaseFour)) Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
          else Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
        case IncomeSourceModel(_, _, true) =>
          Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
        case _ =>
          throw new InternalServerException("User is missing income source type in Subscription Details")
      }
    }
  }

  lazy val backUrl: String =
    controllers.agent.routes.CheckYourAnswersController.show().url
}
