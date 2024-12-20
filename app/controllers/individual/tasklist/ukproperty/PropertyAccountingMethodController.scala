/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.tasklist.ukproperty

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingMethodPropertyForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.ukproperty.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyAccountingMethodController @Inject()(view: PropertyAccountingMethod,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   referenceRetrieval: ReferenceRetrieval)
                                                  (val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val appConfig: AppConfig)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        subscriptionDetailsService.fetchAccountingMethodProperty(reference).map { accountingMethodProperty =>
          Ok(view(
            accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(accountingMethodProperty),
            postAction = routes.PropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(isEditMode, isGlobalEdit)
          ))
        }
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              accountingMethodForm = formWithErrors,
              postAction = routes.PropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
              backUrl = backUrl(isEditMode, isGlobalEdit)))),
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) map {
              case Right(_) => Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
              case Left(_) => throw new InternalServerException("[PropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit).url
    } else {
      routes.PropertyStartDateController.show().url
    }
  }

}
