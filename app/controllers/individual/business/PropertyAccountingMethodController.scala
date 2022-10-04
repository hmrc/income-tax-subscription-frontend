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
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingMethodPropertyForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
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
  def view(accountingMethodPropertyForm: Form[AccountingMethod], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
      propertyAccountingMethod(
        accountingMethodForm = accountingMethodPropertyForm,
        postAction = controllers.individual.business.routes.PropertyAccountingMethodController.submit(editMode = isEditMode),
        isEditMode,
        backUrl = backUrl(isEditMode)
      )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchAccountingMethodProperty(reference).map { accountingMethodProperty =>
          Ok(view(
            accountingMethodPropertyForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethodProperty),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingMethodPropertyForm = formWithErrors, isEditMode = isEditMode))),
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) map {
              case Right(_) => Redirect(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(isEditMode))
              case Left(_) => throw new InternalServerException("[PropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
    } else {
      controllers.individual.business.routes.PropertyStartDateController.show().url
    }
  }
}
