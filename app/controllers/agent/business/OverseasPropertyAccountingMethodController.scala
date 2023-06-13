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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.AccountingMethodOverseasPropertyForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.business.OverseasPropertyAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyAccountingMethodController @Inject()(val auditingService: AuditingService,
                                                           val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                                           overseasPropertyAccountingMethod: OverseasPropertyAccountingMethod)
                                                          (implicit val ec: ExecutionContext,
                                                           val appConfig: AppConfig,
                                                           mcc: MessagesControllerComponents) extends AuthenticatedController with ReferenceRetrieval {
  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyAccountingMethod(reference) flatMap { accountingMethodOverseasProperty =>
          Future.successful(Ok(view(accountingMethodOverseasPropertyForm =
            AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.fill(accountingMethodOverseasProperty),
            isEditMode = isEditMode)))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingMethodOverseasPropertyForm = formWithErrors, isEditMode = isEditMode))),
          overseasPropertyAccountingMethod => {
            subscriptionDetailsService.saveOverseasAccountingMethodProperty(reference, overseasPropertyAccountingMethod) map {
              case Right(_) => Redirect(controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
              case Left(_) => throw new InternalServerException("[OverseasPropertyAccountingMethodController][submit] - Could not save accounting method")
            }
          }
        )
      }
  }

  private def view(accountingMethodOverseasPropertyForm: Form[AccountingMethod], isEditMode: Boolean)
                  (implicit request: Request[AnyContent]): Html = {
    overseasPropertyAccountingMethod(
      accountingMethodOverseasPropertyForm = accountingMethodOverseasPropertyForm,
      postAction = controllers.agent.business.routes.OverseasPropertyAccountingMethodController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = request.clientDetails
    )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.business.routes.OverseasPropertyStartDateController.show().url
    }
  }
}
