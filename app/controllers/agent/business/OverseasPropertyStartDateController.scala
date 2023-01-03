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
import forms.agent.OverseasPropertyStartDateForm
import forms.agent.OverseasPropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.agent.business.OverseasPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(val auditingService: AuditingService,
                                                    overseasPropertyStartDate: OverseasPropertyStartDate,
                                                    val authService: AuthService,
                                                    val subscriptionDetailsService: SubscriptionDetailsService,
                                                    val languageUtils: LanguageUtils)
                                                   (implicit val ec: ExecutionContext,
                                                    val appConfig: AppConfig,
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
                  Redirect(controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show())
                }
              case Left(_) => throw new InternalServerException("[OverseasPropertyStartDateController][submit] - Could not save start date")
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
    }
  }

  private def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean)
                  (implicit request: Request[_]): Html = {
    overseasPropertyStartDate(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      backUrl(isEditMode),
      isEditMode)
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate, OverseasPropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
