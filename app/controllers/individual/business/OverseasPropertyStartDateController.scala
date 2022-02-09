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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.utils.IndividualAnswers._
import controllers.utils.{ReferenceRetrieval, RequireAnswer}
import forms.individual.business.OverseasPropertyStartDateForm
import forms.individual.business.OverseasPropertyStartDateForm._
import models.DateModel
import models.common.IncomeSourceModel
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
  extends SignUpController with ImplicitDateFormatter with RequireAnswer with FeatureSwitching with ReferenceRetrieval {

  private def isSaveAndRetrieve: Boolean = isEnabled(SaveAndRetrieve)

  def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: Option[IncomeSourceModel])
          (implicit request: Request[_]): Html = {

    overseasPropertyStartDateView(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      postAction = controllers.individual.business.routes.OverseasPropertyStartDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel),
      isSaveAndRetrieve = isSaveAndRetrieve
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user => {
      withReference { reference =>
        if (!isSaveAndRetrieve) {
          subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) flatMap { overseasPropertyStartDate =>
            subscriptionDetailsService.fetchIncomeSource(reference) map {
              case Some(incomeSource) => Ok(view(
                overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate),
                isEditMode = isEditMode,
                incomeSourceModel = Some(incomeSource)
              ))
              case None => Redirect(controllers.individual.incomesource.routes.IncomeSourceController.show())
            }
          }
        } else {
          subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) flatMap { overseasPropertyStartDate =>
            Future.successful(Ok(view(
              overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate),
              isEditMode = isEditMode, None)))
          }
        }
      }
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
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
                case (true, true) => controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(isEditMode)
                case (true, false) => controllers.individual.subscription.routes.CheckYourAnswersController.show
                case (false, _) => controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show()
              }
              Future.successful(Redirect(redirectUrl))
            }
        )
      }
  }

  def backUrl(isEditMode: Boolean, maybeIncomeSourceModel: Option[IncomeSourceModel]): String = {
    (isEditMode, isSaveAndRetrieve, maybeIncomeSourceModel) match {
      case (true, true, _) => controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
      case (false, true, _) => controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show.url
      case (true, false, _) => controllers.individual.subscription.routes.CheckYourAnswersController.show.url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.ukProperty =>
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
      case (false, false, Some(incomeSourceModel)) if incomeSourceModel.selfEmployment =>
        appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
      case _ => controllers.individual.incomesource.routes.IncomeSourceController.show().url
    }
  }

  def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate.toLongDate, OverseasPropertyStartDateForm.maxStartDate.toLongDate)
  }
}
