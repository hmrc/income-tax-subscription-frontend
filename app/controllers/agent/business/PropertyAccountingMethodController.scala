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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingMethodPropertyForm
import models.AccountingMethod
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.agent.business.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(propertyAccountingMethod: PropertyAccountingMethod,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val subscriptionDetailsService: SubscriptionDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents,
                                                   val appConfig: AppConfig) extends AuthenticatedController with FeatureSwitching with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

  def view(accountingMethodForm: Form[AccountingMethod], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {

    propertyAccountingMethod(
      accountingMethodForm = accountingMethodForm,
      postAction = controllers.agent.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchAccountingMethodProperty(reference) map { accountingMethod =>
          Ok(view(
            accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethod),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(view(
            accountingMethodForm = formWithErrors,
            isEditMode = isEditMode
          ))),

          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) flatMap { _ =>
              if (isSaveAndRetrieve) {
                Future(Redirect(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode)))
              } else {
                subscriptionDetailsService.fetchIncomeSource(reference) map {
                  case Some(IncomeSourceModel(_, _, true)) =>
                    Redirect(controllers.agent.business.routes.OverseasPropertyStartDateController.show())
                  case _ =>
                    Redirect(controllers.agent.routes.CheckYourAnswersController.show)
                }
              }
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): String = {
    (isEditMode, isSaveAndRetrieve) match {
      case (true, true) => controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode).url
      case (false, _) => controllers.agent.business.routes.PropertyStartDateController.show().url
      case (true, false) => controllers.agent.routes.CheckYourAnswersController.show.url
    }

  }

}
