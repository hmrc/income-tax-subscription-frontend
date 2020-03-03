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

package controllers.individual.business

import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.CacheUtil.CacheMapUtil
import core.services.{AuthService, KeystoreService}
import forms.individual.business.AccountingMethodForm
import javax.inject.{Inject, Singleton}
import models.individual.business.{AccountingMethodModel, MatchTaxYearModel}
import models.individual.incomesource.RentUkPropertyModel
import models.individual.subscription.Business
import models.{No, Yes}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService
                                                  )(implicit val ec: ExecutionContext) extends SignUpController {

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
      keystoreService.fetchAccountingMethod() flatMap { accountingMethod =>
        view(accountingMethodForm = AccountingMethodForm.accountingMethodForm.fill(accountingMethod), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodForm.accountingMethodForm.bindFromRequest.fold(
        formWithErrors => view(accountingMethodForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethod => {
          keystoreService.saveAccountingMethod(accountingMethod) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
            } else {
              keystoreService.fetchRentUkProperty() map {
                case Some(RentUkPropertyModel(Yes, _)) =>
                  Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
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
      keystoreService.fetchAll() map { cacheMap =>
        (cacheMap.getIncomeSourceType(), cacheMap.getMatchTaxYear()) match {
          case (_, Some(MatchTaxYearModel(No))) =>
            controllers.individual.business.routes.BusinessAccountingPeriodDateController.show().url
          case (Some(Business), _) =>
            controllers.individual.business.routes.WhatYearToSignUpController.show().url
          case (_, _) =>
            controllers.individual.business.routes.MatchTaxYearController.show().url
        }
      }
    }


}
