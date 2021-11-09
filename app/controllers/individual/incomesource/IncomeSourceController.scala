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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import connectors.IncomeTaxSubscriptionConnector
import forms.individual.incomesource.IncomeSourceForm
import javax.inject.{Inject, Singleton}
import models.IndividualSummary
import models.common.IncomeSourceModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.SubscriptionDataUtil._
import views.html.individual.incometax.incomesource.IncomeSource

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceController @Inject()(incomeSource: IncomeSource,
                                       val auditingService: AuditingService,
                                       val authService: AuthService,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(incomeSourceForm: Form[IncomeSourceModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    incomeSource(
      incomeSourceForm = incomeSourceForm,
      postAction = controllers.individual.incomesource.routes.IncomeSourceController.submit(editMode = isEditMode),
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
            Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
          case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(releaseFourEnabled = true) =>
            Redirect(controllers.individual.business.routes.PropertyStartDateController.show())
          case IncomeSourceModel(_, _, true) if !summaryModel.foreignPropertyComplete =>
            Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
          case _ =>
            Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
        }
      } else {
        incomeSource match {
          case IncomeSourceModel(true, _, _) if !summaryModel.selfEmploymentComplete(releaseFourEnabled = false) =>
            Redirect(controllers.individual.business.routes.BusinessNameController.show())
          case IncomeSourceModel(_, true, _) if !summaryModel.ukPropertyComplete(releaseFourEnabled = false) =>
            Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
          case _ =>
            Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
        }
      }
    }
  }

  private def getSummaryModel(cacheMap: CacheMap)(implicit hc: HeaderCarrier): Future[IndividualSummary] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](BusinessAccountingMethod)
      property <- subscriptionDetailsService.fetchProperty()
    } yield {
      if (isEnabled(ReleaseFour)) {
        cacheMap.getSummary(businesses, businessAccountingMethod, property, isReleaseFourEnabled = true)
      } else {
        cacheMap.getSummary(property = property)
      }
    }
  }

  private def linearJourney(incomeSource: IncomeSourceModel)(implicit request: Request[_]): Future[Result] = {
    subscriptionDetailsService.saveIncomeSource(incomeSource) map { _ =>
      incomeSource match {
        case IncomeSourceModel(true, _, _) =>
          if (isEnabled(ReleaseFour)) Redirect(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
          else Redirect(controllers.individual.business.routes.BusinessNameController.show())
        case IncomeSourceModel(_, true, _) =>
          if (isEnabled(ReleaseFour)) Redirect(controllers.individual.business.routes.PropertyStartDateController.show())
          else Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
        case IncomeSourceModel(_, _, true) if isEnabled(ForeignProperty) =>
          Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
        case _ =>
          throw new InternalServerException("[IncomeSourceController][linearJourney] - Unexpected income source state")
      }
    }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else {
      controllers.individual.business.routes.WhatYearToSignUpController.show().url
    }
  }
}
