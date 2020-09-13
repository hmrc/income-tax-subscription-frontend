/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.utils.Answers._
import controllers.utils.RequireAnswer
import forms.individual.business.OverseasPropertyCommencementDateForm
import forms.individual.business.OverseasPropertyCommencementDateForm._
import javax.inject.{Inject, Singleton}
import models.common.IncomeSourceModel
import models.individual.business.OverseasPropertyCommencementDateModel
import play.api.data.Form
import play.api.libs.functional.~
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.individual.ImplicitDateFormatter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyCommencementDateController @Inject()(val authService: AuthService,
                                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                                           val languageUtils: LanguageUtils)(implicit val ec: ExecutionContext,
                                                            appConfig: AppConfig, mcc: MessagesControllerComponents) extends
                                                            SignUpController with ImplicitDateFormatter with RequireAnswer {

  def view(overseasPropertyCommencementDateForm: Form[OverseasPropertyCommencementDateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
          (implicit request: Request[_]): Html = {
    views.html.individual.incometax.business.overseas_property_commencement_date(
      overseasPropertyCommencementDateForm = overseasPropertyCommencementDateForm,
      postAction = controllers.individual.business.routes.OverseasPropertyCommencementDateController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode, incomeSourceModel)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      require(optForeignPropertyCommencementDateAnswer and incomeSourceModelAnswer) {
        case foreignPropertyCommencementDate ~ incomeSource =>
          Future.successful(Ok(view(
            overseasPropertyCommencementDateForm = form.fill(foreignPropertyCommencementDate),
            isEditMode = isEditMode,
            incomeSourceModel = incomeSource
          )))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      form.bindFromRequest.fold(
        formWithErrors =>
          require(incomeSourceModelAnswer) {
            incomeSourceModel =>
              Future.successful(BadRequest(view(overseasPropertyCommencementDateForm = formWithErrors, isEditMode = isEditMode, incomeSourceModel)))
          },
        startDate =>
          subscriptionDetailsService.saveOverseasPropertyCommencementDate(startDate) flatMap { _ =>
            if (isEditMode) {
              Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show()))
            } else {
              Future.successful(Redirect(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show()))
            }
          }

      )
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)(implicit hc: HeaderCarrier): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else {
      (incomeSourceModel.selfEmployment, incomeSourceModel.ukProperty) match {
        case (_, true) => appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/business/accounting-method-property"
        case (true, false) => appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/details/business-accounting-method"
        case _ => controllers.individual.incomesource.routes.IncomeSourceController.show().url
      }
    }
  }


  def form(implicit request: Request[_]): Form[OverseasPropertyCommencementDateModel] = {
    overseasPropertyCommencementDateForm(OverseasPropertyCommencementDateForm.overseasPropertyStartDate.toLongDate)
  }

}
