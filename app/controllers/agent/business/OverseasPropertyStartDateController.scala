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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.AgentAnswers._
import controllers.utils.{ReferenceRetrieval, RequireAnswer}
import forms.agent.OverseasPropertyStartDateForm
import forms.agent.OverseasPropertyStartDateForm._
import models.DateModel
import models.common.IncomeSourceModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
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
  extends AuthenticatedController with ImplicitDateFormatter with RequireAnswer  with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

  def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: Option[IncomeSourceModel])
          (implicit request: Request[_]): Html = {
    overseasPropertyStartDate(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      backUrl(isEditMode, incomeSourceModel),
      isEditMode)
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) flatMap { overseasPropertyStartDate =>
          if (isSaveAndRetrieve) {
            Future.successful(Ok(view(
              overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate), isEditMode, None
            )))
          } else {
            subscriptionDetailsService.fetchIncomeSource(reference) map {
              case Some(incomeSource) => Ok(view(
                overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate), isEditMode, Some(incomeSource)
              ))
              case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
            }
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest.fold(
          formWithErrors =>
            if (!isSaveAndRetrieve) {
              require(reference)(incomeSourceModelAnswer) { incomeSourceModel =>
                Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode, Some(incomeSourceModel))))
              }
            } else {
              Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode, None)))
            },
          startDate =>
            subscriptionDetailsService.saveOverseasPropertyStartDate(reference, startDate) flatMap { _ =>

              val redirectUrl = (isEditMode, isSaveAndRetrieve) match {
                case (true, true) => controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode)
                case (true, false) => controllers.agent.routes.CheckYourAnswersController.show
                case (false, _) => controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show()
              }
              Future.successful(Redirect(redirectUrl))
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean, maybeIncomeSourceModel: Option[IncomeSourceModel]): String = {
    (isEditMode, isSaveAndRetrieve, maybeIncomeSourceModel) match {
      case (true, true, _) => controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
      case (false, true, _) => controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      case (true, false, _) => controllers.agent.routes.CheckYourAnswersController.show.url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.ukProperty =>
        controllers.agent.business.routes.PropertyAccountingMethodController.show().url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.selfEmployment =>
        appConfig.incomeTaxSelfEmploymentsFrontendUrl + "client/details/business-accounting-method"
      case _ => controllers.agent.routes.IncomeSourceController.show().url
    }
  }

  def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate, OverseasPropertyStartDateForm.maxStartDate, d => d.toLongDate)
  }
}
