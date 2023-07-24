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
import forms.agent.AccountingMethodPropertyForm
import forms.agent.UkPropertyCountForm
import models.AccountingMethod
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.business.UkPropertyCount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPropertyCountController @Inject()(ukPropertyCount: UkPropertyCount,
                                          val auditingService: AuditingService,
                                          val authService: AuthService,
                                          val subscriptionDetailsService: SubscriptionDetailsService)
                                         (implicit val ec: ExecutionContext,
                                          mcc: MessagesControllerComponents,
                                          val appConfig: AppConfig) extends AuthenticatedController with ReferenceRetrieval {

  def view(countForm: Form[Int], isEditMode: Boolean)
          (implicit request: Request[AnyContent]): Html = {
    ukPropertyCount(
      countForm = countForm,
      postAction = controllers.agent.business.routes.UkPropertyCountController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = request.clientDetails
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchUkPropertyCount(reference) map { count =>
          Ok(view(
            countForm = UkPropertyCountForm.form.fill(count),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        UkPropertyCountForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(countForm = formWithErrors, isEditMode = isEditMode))),
          count => {
            subscriptionDetailsService.saveUkPropertyCount(reference, count) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(controllers.agent.business.routes.PropertyAccountingMethodController.show(isEditMode))
                }
              case Left(_) => throw new InternalServerException("[PropertyCountController][submit] - Could not save number of properties")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.business.routes.PropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.business.routes.PropertyStartDateController.show().url
    }
  }

}
