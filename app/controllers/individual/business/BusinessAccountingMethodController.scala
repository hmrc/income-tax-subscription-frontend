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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.ForeignProperty
import config.featureswitch.FeatureSwitching
import forms.individual.business.AccountingMethodForm
import javax.inject.{Inject, Singleton}
import models.common.IncomeSourceModel
import models.common.business.AccountingMethodModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataUtil.CacheMapUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingMethodController @Inject()(val authService: AuthService, subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext, appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(accountingMethodForm: Form[AccountingMethodModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      views.html.individual.incometax.business.accounting_method(
        accountingMethodForm = accountingMethodForm,
        postAction = controllers.individual.business.routes.BusinessAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchAccountingMethod() flatMap { accountingMethod =>
        view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => view(accountingMethodForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethod => {
          subscriptionDetailsService.saveAccountingMethod(accountingMethod) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
            } else {
              subscriptionDetailsService.fetchIncomeSource() map {
                case Some(IncomeSourceModel(_, true, _)) =>
                  Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
                case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty)  =>
                  Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
                case _ =>
                  Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
              }
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] =
    if (isEditMode)
      Future.successful(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)
    else {
      subscriptionDetailsService.fetchAll() map { cacheMap =>
        cacheMap.getIncomeSource match {
          case Some(IncomeSourceModel(true, false, _)) =>
            controllers.individual.business.routes.WhatYearToSignUpController.show().url
          case _ =>
            controllers.individual.business.routes.BusinessNameController.show().url
        }
      }
    }
}