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
import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import controllers.utils.ReferenceRetrieval
import forms.individual.business.AccountingMethodPropertyForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.ukproperty.PropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

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
        subscriptionDetailsService.fetchProperty(reference).map { property =>
          Ok(view(
            accountingMethodForm = AccountingMethodPropertyForm.accountingMethodPropertyForm.fill(property.flatMap(_.accountingMethod)),
            postAction = routes.PropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(
              isEditMode = isEditMode,
              isGlobalEdit = isGlobalEdit,
              maybeStartDateBeforeLimit = property.flatMap(_.startDateBeforeLimit)
            )
          ))
        }
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        AccountingMethodPropertyForm.accountingMethodPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            subscriptionDetailsService.fetchProperty(reference).map { property =>
              BadRequest(view(
                accountingMethodForm = formWithErrors,
                postAction = routes.PropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
                backUrl = backUrl(
                  isEditMode = isEditMode,
                  isGlobalEdit = isGlobalEdit,
                  maybeStartDateBeforeLimit = property.flatMap(_.startDateBeforeLimit)
                )
              ))
            },
          accountingMethodProperty => {
            subscriptionDetailsService.saveAccountingMethodProperty(reference, accountingMethodProperty) map {
              case Right(_) => Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
              case Left(_) => throw new InternalServerException("[PropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean, maybeStartDateBeforeLimit: Option[Boolean]): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = isGlobalEdit).url
    } else {
      if (isEnabled(StartDateBeforeLimit)) {
        if (maybeStartDateBeforeLimit.contains(false)) {
          routes.PropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
        } else {
          routes.PropertyStartDateBeforeLimitController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
        }
      } else {
        routes.PropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
      }
    }
  }

}
