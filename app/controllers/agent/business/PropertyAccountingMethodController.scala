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

import auth.agent.{AuthenticatedController, UserMatchingController}
import config.AppConfig
import forms.agent.AccountingMethodPropertyForm
import javax.inject.{Inject, Singleton}
import models.agent.AccountingMethodPropertyModel
import models.individual.subscription.{Both, Property}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.AuthService
import services.agent.KeystoreService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.agent.CacheUtil.CacheMapUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(val authService: AuthService, keystoreService: KeystoreService)
                                                  (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                                   appConfig: AppConfig) extends AuthenticatedController {

  def view(accountingMethodPropertyForm: Form[AccountingMethodPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Future[Html] = {
    for {
      back <- backUrl(isEditMode)
    } yield
      views.html.agent.business.property_accounting_method(
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
            Redirect(controllers.agent.routes.CheckYourAnswersController.show())
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
        case Some(cacheMap) => cacheMap.getIncomeSource() match {
          case Some(Property) => controllers.agent.routes.IncomeSourceController.show().url
          case Some(Both) => controllers.agent.business.routes.BusinessAccountingMethodController.show().url
          case _ => controllers.agent.routes.IncomeSourceController.show().url
        }
      }
    }

}
