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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.OverseasPropertyStartDateForm
import forms.individual.business.OverseasPropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import views.html.individual.incometax.business.OverseasPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(val auditingService: AuditingService,
                                                    val authService: AuthService,
                                                    val subscriptionDetailsService: SubscriptionDetailsService,
                                                    val languageUtils: LanguageUtils,
                                                    val overseasPropertyStartDateView: OverseasPropertyStartDate)
                                                   (implicit val ec: ExecutionContext,
                                                    val appConfig: AppConfig,
                                                    mcc: MessagesControllerComponents)
  extends SignUpController with ImplicitDateFormatter with ReferenceRetrieval {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) flatMap { overseasPropertyStartDate =>
          Future.successful(Ok(view(
            overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate),
            isEditMode = isEditMode)
          ))
        }
      }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        form.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode))),
          startDate =>
            subscriptionDetailsService.saveOverseasPropertyStartDate(reference, startDate) flatMap { _ =>
              val redirectUrl = if (isEditMode) {
                controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode)
              } else {
                controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show()
              }

              Future.successful(Redirect(redirectUrl))
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
    } else {
      controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show.url
    }
  }

  private def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean)
                  (implicit request: Request[_]): Html = {
    overseasPropertyStartDateView(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      postAction = controllers.individual.business.routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate, OverseasPropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
