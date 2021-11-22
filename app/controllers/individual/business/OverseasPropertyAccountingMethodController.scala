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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import forms.individual.business._
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.individual.incometax.business.OverseasPropertyAccountingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OverseasPropertyAccountingMethodController @Inject()(val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                                           overseasPropertyAccountingMethod: OverseasPropertyAccountingMethod)
                                                          (implicit val ec: ExecutionContext,
                                                           val appConfig: AppConfig,
                                                           mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

  def view(overseasPropertyAccountingMethodForm: Form[AccountingMethod], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    overseasPropertyAccountingMethod(
      overseasPropertyAccountingMethodForm = overseasPropertyAccountingMethodForm,
      postAction = controllers.individual.business.routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      isSaveAndRetrieve = isEnabled(SaveAndRetrieve),
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyAccountingMethod(reference) flatMap { accountingMethodOverseasProperty =>
          Future.successful(Ok(view(overseasPropertyAccountingMethodForm =
            AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(accountingMethodOverseasProperty),
            isEditMode = isEditMode)))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(view(overseasPropertyAccountingMethodForm = formWithErrors, isEditMode = isEditMode))),
          overseasPropertyAccountingMethod =>
            subscriptionDetailsService.saveOverseasAccountingMethodProperty(reference, overseasPropertyAccountingMethod) map { _ =>
              if (isSaveAndRetrieve) {
                Redirect(controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
              } else {
                Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
              }
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): String = {
    (isEditMode, isSaveAndRetrieve) match {
      case (true, true) => controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
      case (false, _) => controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
      case (true, false) => controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    }
  }
}
