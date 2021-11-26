/*
 * Copyright 2021 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import forms.agent.IncomeSourceForm
import models.AgentSummary
import models.common.IncomeSourceModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil.CacheMapUtil
import views.html.agent.IncomeSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceController @Inject()(
                                        incomeSource: IncomeSource,
                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends AuthenticatedController with FeatureSwitching {

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      controllers.agent.routes.WhatYearToSignUpController.show().url
    }
  }

  def view(incomeSourceForm: Form[IncomeSourceModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    incomeSource(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.agent.routes.IncomeSourceController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      foreignProperty = isEnabled(ForeignProperty)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchIncomeSource() map { incomeSource =>
        Ok(view(incomeSourceForm = incomeSourceForm.fill(incomeSource), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      incomeSourceForm.bindFromRequest.fold(
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

  private def incomeSourceForm: Form[IncomeSourceModel] = IncomeSourceForm.incomeSourceForm(isEnabled(ForeignProperty))

  private[controllers] def editJourney(incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      cacheMap <- subscriptionDetailsService.saveIncomeSource(incomeSource)
      summaryModel <- getSummaryModel(cacheMap)
    } yield {

      if (isEnabled(ReleaseFour)) {

        incomeSource match {
          case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = true) =>
            Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
          case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(true) =>
            Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
          case IncomeSourceModel(_, _, true) if !summaryModel.foreignPropertyComplete =>
            Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
          case _ =>
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
        }

      } else {
        incomeSource match {
          case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = false) =>
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
      property <- subscriptionDetailsService.fetchProperty()
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty()
    } yield {
      if (isEnabled(ReleaseFour)) {
        cacheMap.getAgentSummary(businesses, businessAccountingMethod, property, overseasProperty)
      } else {
        cacheMap.getAgentSummary()
      }
    }
  }

  private def linearJourney(incomeSource: IncomeSourceModel)(implicit request: Request[_]): Future[Result] = {
    subscriptionDetailsService.saveIncomeSource(incomeSource) map { _ =>
      incomeSource match {
        case IncomeSourceModel(true, _, _) =>
          if (isEnabled(ReleaseFour)) Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
          else Redirect(controllers.agent.business.routes.BusinessNameController.show())
        case IncomeSourceModel(_, true, _) =>
          if (isEnabled(ReleaseFour)) Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
          else Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show())
        case IncomeSourceModel(_, _, true) if isEnabled(ForeignProperty) =>
          Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
        case _ =>
          throw new InternalServerException("User is missing income source type in Subscription Details")
      }
    }
  }
}
