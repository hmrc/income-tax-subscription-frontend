/*
 * Copyright 2021 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitching
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
  extends AuthenticatedController with ImplicitDateFormatter with RequireAnswer with FeatureSwitching with ReferenceRetrieval {

  def view(overseasPropertyStartDateForm: Form[DateModel], isEditMode: Boolean, incomeSourceModel: IncomeSourceModel)
          (implicit request: Request[_]): Html = {
    overseasPropertyStartDate(
      overseasPropertyStartDateForm = overseasPropertyStartDateForm,
      routes.OverseasPropertyStartDateController.submit(),
      backUrl(isEditMode, incomeSourceModel),
      isEditMode)
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchOverseasPropertyStartDate(reference) flatMap { overseasPropertyStartDate =>
          subscriptionDetailsService.fetchIncomeSource(reference) map {
            case Some(incomeSourceModel) => Ok(view(
              overseasPropertyStartDateForm = form.fill(overseasPropertyStartDate), isEditMode, incomeSourceModel
            ))
            case None => Redirect(controllers.agent.routes.IncomeSourceController.show())
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        form.bindFromRequest.fold(
          formWithErrors =>
            require(reference)(incomeSourceModelAnswer) {
              incomeSourceModel =>
                Future.successful(BadRequest(view(overseasPropertyStartDateForm = formWithErrors, isEditMode = isEditMode, incomeSourceModel = incomeSourceModel)))
            },
          startDate =>
            subscriptionDetailsService.saveOverseasPropertyStartDate(reference, startDate) flatMap { _ =>
              if (isEditMode) {
                Future.successful(Redirect(controllers.agent.routes.CheckYourAnswersController.show()))
              } else {
                Future.successful(Redirect(controllers.agent.business.routes.OverseasPropertyAccountingMethodController.submit()))
              }
            }

        )
      }
  }

  def backUrl(isEditMode: Boolean, incomeSourceModel: IncomeSourceModel): String = {
    if (isEditMode) {
      controllers.agent.routes.CheckYourAnswersController.show().url
    } else {
      (incomeSourceModel.selfEmployment, incomeSourceModel.ukProperty) match {
        case (_, true) => controllers.agent.business.routes.PropertyAccountingMethodController.show().url
        case (true, _) => appConfig.incomeTaxSelfEmploymentsFrontendUrl + "/client/details/business-accounting-method"
        case _ => controllers.agent.routes.IncomeSourceController.show().url
      }
    }
  }


  def form(implicit request: Request[_]): Form[DateModel] = {
    overseasPropertyStartDateForm(OverseasPropertyStartDateForm.minStartDate.toLongDate, OverseasPropertyStartDateForm.maxStartDate.toLongDate)
  }

}
