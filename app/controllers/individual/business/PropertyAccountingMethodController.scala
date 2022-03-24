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
import config.featureswitch.FeatureSwitch.{ForeignProperty, SaveAndRetrieve}
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingMethodPropertyForm
import models.AccountingMethod
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import views.html.individual.incometax.business.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val auditingService: AuditingService,
                                                   propertyAccountingMethod: PropertyAccountingMethod,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController  with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

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
      withReference { reference =>
        subscriptionDetailsService.fetchAccountingMethodProperty(reference) flatMap { accountingMethodProperty =>
          view(
            accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethodProperty),
            isEditMode = isEditMode,
            isSaveAndRetrieve = isEnabled(SaveAndRetrieve)
          ).map(view => Ok(view))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
          formWithErrors =>
            view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode, isEnabled(SaveAndRetrieve)).map(view => BadRequest(view)),
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) flatMap { _ =>
              (isEditMode, isSaveAndRetrieve) match {
                case (_, true) => Future(Redirect(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(isEditMode)))
                case (_, false) => subscriptionDetailsService.fetchIncomeSource(reference) map {
                  case Some(IncomeSourceModel(_, _, true)) if isEnabled(ForeignProperty) =>
                    Redirect(controllers.individual.business.routes.OverseasPropertyStartDateController.show())
                  case _ =>
                    Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show)
                }
              }
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): Future[String] = {
    (isEditMode, isSaveAndRetrieve) match {
      case (true, true) => Future.successful(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url)
      case (false, _) => Future.successful(controllers.individual.business.routes.PropertyStartDateController.show().url)
      case (true, false) => Future.successful(controllers.individual.subscription.routes.CheckYourAnswersController.show.url)
    }

  }
}
