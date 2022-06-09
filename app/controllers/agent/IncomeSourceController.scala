/*
 * Copyright 2022 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitch.ForeignProperty
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
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
class IncomeSourceController @Inject()(incomeSource: IncomeSource,
                                       val auditingService: AuditingService,
                                       val authService: AuthService,
                                       val subscriptionDetailsService: SubscriptionDetailsService,
                                       incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends AuthenticatedController
  with ReferenceRetrieval {

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show.url
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
      withAgentReference { reference =>
        subscriptionDetailsService.fetchIncomeSource(reference) map { incomeSource =>
          Ok(view(incomeSourceForm = incomeSourceForm.fill(incomeSource), isEditMode = isEditMode))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        incomeSourceForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(view(incomeSourceForm = formWithErrors, isEditMode = isEditMode))),
          incomeSource => {
            if (!isEditMode) {
              linearJourney(reference, incomeSource)
            } else {
              editJourney(reference, incomeSource)
            }
          }
        )
      }
  }

  private def incomeSourceForm: Form[IncomeSourceModel] = IncomeSourceForm.incomeSourceForm(isEnabled(ForeignProperty))

  private def editJourney(reference: String, incomeSource: IncomeSourceModel)(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      cacheMap <- subscriptionDetailsService.saveIncomeSource(reference, incomeSource)
      summaryModel <- getSummaryModel(reference, cacheMap)
    } yield {
      incomeSource match {
        case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete =>
          Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete =>
          Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
        case IncomeSourceModel(_, _, true) if !summaryModel.foreignPropertyComplete =>
          Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
        case _ =>
          Redirect(controllers.agent.routes.CheckYourAnswersController.show)
      }
    }
  }

  private def getSummaryModel(reference: String, cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[AgentSummary] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
    } yield {
      cacheMap.getAgentSummary(businesses, businessAccountingMethod, property, overseasProperty)
    }
  }

  private def linearJourney(reference: String, incomeSource: IncomeSourceModel)(implicit request: Request[_]): Future[Result] = {
    subscriptionDetailsService.saveIncomeSource(reference, incomeSource) map { _ =>
      incomeSource match {
        case IncomeSourceModel(true, _, _) => Redirect(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
        case IncomeSourceModel(_, true, _) => Redirect(controllers.agent.business.routes.PropertyStartDateController.show())
        case IncomeSourceModel(_, _, true) if isEnabled(ForeignProperty) =>
          Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
        case _ =>
          throw new InternalServerException("User is missing income source type in Subscription Details")
      }
    }
  }
}
