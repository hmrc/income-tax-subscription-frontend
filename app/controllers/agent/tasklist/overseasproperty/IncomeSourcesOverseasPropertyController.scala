/*
 * Copyright 2024 HM Revenue & Customs
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
import config.featureswitch.FeatureSwitching
import controllers.utils.ReferenceRetrieval
import forms.agent.IncomeSourcesOverseasPropertyForm
import models.common.OverseasPropertyModel
import models.{AccountingMethod, DateModel}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.language.LanguageUtils
import utilities.ImplicitDateFormatter
import utilities.UserMatchingSessionUtil.ClientDetails
import views.html.agent.tasklist.overseasproperty.IncomeSourcesOverseasProperty

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IncomeSourcesOverseasPropertyController @Inject()(incomeSourcesOverseasProperty: IncomeSourcesOverseasProperty,
                                                        referenceRetrieval: ReferenceRetrieval,
                                                        clientDetailsRetrieval: ClientDetailsRetrieval,
                                                        subscriptionDetailsService: SubscriptionDetailsService)
                                                       (val auditingService: AuditingService,
                                                        val authService: AuthService,
                                                        val appConfig: AppConfig,
                                                        val languageUtils: LanguageUtils)
                                                       (implicit val ec: ExecutionContext,
                                                        mcc: MessagesControllerComponents) extends AuthenticatedController
  with ImplicitDateFormatter with FeatureSwitching {

  private def form(implicit request: Request[_]): Form[(DateModel, AccountingMethod)] = {
    IncomeSourcesOverseasPropertyForm.incomeSourcesOverseasPropertyForm(_.toLongDate)
  }

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getAgentReference
        maybeOverseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
        clientDetails <- clientDetailsRetrieval.getClientDetails
      } yield {
        val formData: Map[String, String] = IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(
          maybeOverseasProperty.flatMap(_.startDate),
          maybeOverseasProperty.flatMap(_.accountingMethod)
        )
        val boundForm = form.bind(formData).discardingErrors
        Ok(view(
          overseasPropertyForm = boundForm,
          isEditMode = isEditMode,
          isGlobalEdit = isGlobalEdit,
          clientDetails = clientDetails
        ))
      }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors => clientDetailsRetrieval.getClientDetails map { clientDetails =>
            BadRequest(view(
              overseasPropertyForm = formWithErrors,
              isEditMode = isEditMode,
              isGlobalEdit = isGlobalEdit,
              clientDetails = clientDetails
            ))
          }, {
            case (startDate, accountingMethod) =>
              val overseasPropertyModel = OverseasPropertyModel(accountingMethod = Some(accountingMethod), startDate = Some(startDate))
              subscriptionDetailsService.saveOverseasProperty(reference, overseasPropertyModel) map {
                case Right(_) => Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
                case Left(_) => throw new InternalServerException("[IncomeSourcesOverseasPropertyController][submit] - Could not save overseas property")
              }
          }
        )
      }
  }

  private def view(overseasPropertyForm: Form[(DateModel, AccountingMethod)], isEditMode: Boolean, isGlobalEdit: Boolean, clientDetails: ClientDetails)
                  (implicit request: Request[AnyContent]): Html = {
    incomeSourcesOverseasProperty(
      incomeSourcesOverseasPropertyForm = overseasPropertyForm,
      postAction = routes.IncomeSourcesOverseasPropertyController.submit(isEditMode, isGlobalEdit),
      backUrl = backUrl(isEditMode, isGlobalEdit),
      clientDetails = clientDetails
    )
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }
}