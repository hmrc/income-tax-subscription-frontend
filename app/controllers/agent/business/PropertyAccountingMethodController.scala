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

package controllers.agent.business

import agent.auth.AuthenticatedController
import agent.forms.AccountingMethodPropertyForm
import agent.models.AccountingMethodPropertyModel
import agent.services.CacheUtil.CacheMapUtil
import agent.services.KeystoreService
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import core.config.{AppConfig, BaseControllerConfig}
import core.models.{No, Yes}
import core.services.AuthService
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.{Both, Property}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class PropertyAccountingMethodController @Inject()(val baseConfig: BaseControllerConfig,
                                                   val messagesApi: MessagesApi,
                                                   val keystoreService: KeystoreService,
                                                   val authService: AuthService,
                                                   val appConfig: AppConfig,
                                                   val currentTimeService: CurrentTimeService
                                                  ) extends AuthenticatedController with FeatureSwitching {

  def view(accountingMethodPropertyForm: Form[AccountingMethodPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      agent.views.html.business.property_accounting_method(
        accountingMethodPropertyForm = accountingMethodPropertyForm,
        postAction = controllers.agent.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = back
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchAccountingMethodProperty() flatMap { accountingMethodProperty =>
        view(accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm
          .fill(accountingMethodProperty), isEditMode = isEditMode).map(view => Ok(view))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode).map(view => BadRequest(view)),
        accountingMethodProperty => {
          keystoreService.saveAccountingMethodProperty(accountingMethodProperty) map { _ =>
            if (isEditMode || isEnabled(EligibilityPagesFeature)) {
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            } else {
              Redirect(controllers.agent.routes.TermsController.show())
            }
          }
        }
      )
  }

  def backUrl(isEditMode: Boolean)(implicit hc: HeaderCarrier): Future[String] =
    if (isEditMode)
      Future.successful(controllers.agent.routes.CheckYourAnswersController.show().url)
    else {
      keystoreService.fetchAll() map {
        case None => controllers.agent.routes.IncomeSourceController.show().url
        case Some(cacheMap) => (cacheMap.getIncomeSource(), cacheMap.getOtherIncome()) match {
          case (Some(Property), _) if isEnabled(EligibilityPagesFeature) => controllers.agent.routes.IncomeSourceController.show().url
          case (Some(Property), Some(Yes)) => controllers.agent.routes.OtherIncomeErrorController.show().url
          case (Some(Property), Some(No)) => controllers.agent.routes.OtherIncomeController.show().url
          case (Some(Both), _) => controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          case _ => controllers.agent.routes.IncomeSourceController.show().url
        }
      }
    }

}
