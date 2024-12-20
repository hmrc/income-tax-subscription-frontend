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

package controllers.individual.tasklist.overseasproperty

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.overseasproperty.OverseasPropertyAccountingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OverseasPropertyAccountingMethodController @Inject()(view: OverseasPropertyAccountingMethod,
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
        subscriptionDetailsService.fetchOverseasPropertyAccountingMethod(reference) map { accountingMethodOverseasProperty =>
          Ok(view(
            overseasPropertyAccountingMethodForm = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm
              .fill(accountingMethodOverseasProperty),
            postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(isEditMode, isGlobalEdit)
          ))
        }
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              overseasPropertyAccountingMethodForm = formWithErrors,
              postAction = routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
              backUrl = backUrl(isEditMode, isGlobalEdit)
            ))),
          overseasPropertyAccountingMethod =>
            subscriptionDetailsService.saveOverseasAccountingMethodProperty(reference, overseasPropertyAccountingMethod) map {
              case Right(_) => Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
              case Left(_) => throw new InternalServerException("[OverseasPropertyAccountingMethodController][submit] - Could not save accounting method")
            }
        )
      }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      controllers.individual.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit).url
    } else {
      routes.OverseasPropertyStartDateController.show().url
    }
  }
}
