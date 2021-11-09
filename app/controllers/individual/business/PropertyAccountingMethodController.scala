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

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour, SaveAndRetrieve}
import config.featureswitch.FeatureSwitching
import forms.individual.business.AccountingMethodPropertyForm
import models.AccountingMethod
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.SubscriptionDataUtil._
import views.html.individual.incometax.business.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val auditingService: AuditingService,
                                                   propertyAccountingMethod: PropertyAccountingMethod,
                                                   val authService: AuthService,
                                                   subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with FeatureSwitching {

  def view(accountingMethodPropertyForm: Form[AccountingMethod], isEditMode: Boolean, isSaveAndRetrieve: Boolean)
          (implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      propertyAccountingMethod(
        accountingMethodForm = accountingMethodPropertyForm,
        postAction = controllers.individual.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back,
        isSaveAndRetrieve
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      subscriptionDetailsService.fetchAccountingMethodProperty() flatMap { accountingMethodProperty =>
        view(
          accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethodProperty),
          isEditMode = isEditMode,
          isSaveAndRetrieve = isEnabled(SaveAndRetrieve)
        ).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode, isEnabled(SaveAndRetrieve)).map(view => BadRequest(view)),
        accountingMethodProperty => {
          subscriptionDetailsService.saveAccountingMethodProperty(accountingMethodProperty) flatMap { _ =>
            if (isEnabled(SaveAndRetrieve)) {
              Future.successful(Redirect(controllers.individual.business.routes.TaskListController.show()))
            } else {
              if (isEditMode) {
                Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
              } else subscriptionDetailsService.fetchIncomeSource() map {
                case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty) =>
                  Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
                case _ =>
                  Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
              }
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] = {
    if (isEnabled(SaveAndRetrieve)) {
      if (isEditMode) {
        Future.successful(controllers.individual.business.routes.TaskListController.show().url)
      } else {
        Future.successful(controllers.individual.business.routes.PropertyStartDateController.show().url)
      }
    } else {
      if (isEditMode) {
        Future.successful(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)
      } else if (isEnabled(ReleaseFour)) {
        Future.successful(controllers.individual.business.routes.PropertyStartDateController.show().url)
      } else {
        subscriptionDetailsService.fetchAll() map { cacheMap =>
          cacheMap.getIncomeSource match {
            case Some(IncomeSourceModel(false, true, _)) =>
              controllers.individual.incomesource.routes.IncomeSourceController.show().url
            case _ =>
              controllers.individual.business.routes.BusinessAccountingMethodController.show().url
          }
        }
      }
    }

  }
}
