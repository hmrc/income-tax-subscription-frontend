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
import forms.agent.OverseasPropertyStartDateForm
import forms.agent.OverseasPropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(overseasPropertyStartDate: OverseasPropertyStartDate)
                                                   (val auditingService: AuditingService,
                                                    val authService: AuthService,
                                                    val subscriptionDetailsService: SubscriptionDetailsService,
                                                    val sessionDataService: SessionDataService,
                                                    val appConfig: AppConfig,
                                                    val languageUtils: LanguageUtils)
                                                   (implicit val ec: ExecutionContext,
                                                    mcc: MessagesControllerComponents)
  extends AuthenticatedController with ImplicitDateFormatter with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) map { overseasPropertyStartDate =>
          Ok(view(
            overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate), isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode))
            ),
          startDate =>
            subscriptionDetailsService.saveOverseasPropertyStartDate(reference, startDate) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(routes.OverseasPropertyAccountingMethodController.show())
                }
              case Left(_) => throw new InternalServerException("[OverseasPropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

  private def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean)
                  (implicit request: Request[AnyContent]): Html = {
    overseasPropertyStartDate(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      backUrl(isEditMode),
      isEditMode,
      clientDetails = request.clientDetails
    )
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate, OverseasPropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
