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

package controllers.agent.tasklist.overseasproperty

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingMethodOverseasPropertyForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.ClientDetails
import views.html.agent.tasklist.overseasproperty.OverseasPropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class OverseasPropertyAccountingMethodController @Inject()(overseasPropertyAccountingMethod: OverseasPropertyAccountingMethod,
                                                           subscriptionDetailsService: SubscriptionDetailsService,
                                                           clientDetailsRetrieval: ClientDetailsRetrieval,
                                                           referenceRetrieval: ReferenceRetrieval)
                                                          (val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val appConfig: AppConfig)
                                                          (implicit val ec: ExecutionContext,
                                                           mcc: MessagesControllerComponents) extends AuthenticatedController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getAgentReference
        clientDetails <- clientDetailsRetrieval.getClientDetails
        accountingMethod <- subscriptionDetailsService.fetchOverseasPropertyAccountingMethod(reference)
      } yield {
        Ok(view(
          accountingMethodOverseasPropertyForm = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(accountingMethod),
          isEditMode = isEditMode,
          clientDetails = clientDetails
        ))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            clientDetailsRetrieval.getClientDetails map { clientDetails =>
              BadRequest(view(accountingMethodOverseasPropertyForm = formWithErrors, isEditMode = isEditMode, clientDetails = clientDetails))
            },
          overseasPropertyAccountingMethod => {
            subscriptionDetailsService.saveOverseasAccountingMethodProperty(reference, overseasPropertyAccountingMethod) map {
              case Right(_) => Redirect(controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
              case Left(_) => throw new InternalServerException("[OverseasPropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  private def view(accountingMethodOverseasPropertyForm: Form[AccountingMethod], isEditMode: Boolean, clientDetails: ClientDetails)
                  (implicit request: Request[AnyContent]): Html = {
    overseasPropertyAccountingMethod(
      accountingMethodOverseasPropertyForm = accountingMethodOverseasPropertyForm,
      postAction = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = clientDetails
    )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
      controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      routes.OverseasPropertyStartDateController.show().url
    }
  }
}
