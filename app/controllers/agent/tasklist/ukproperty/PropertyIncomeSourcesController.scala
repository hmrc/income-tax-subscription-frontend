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

package controllers.agent.tasklist.ukproperty

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.agent.UkPropertyIncomeSourcesForm
import models.common.PropertyModel
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
import views.html.agent.tasklist.ukproperty.PropertyIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PropertyIncomeSourcesController @Inject()(propertyIncomeSources: PropertyIncomeSources,
                                                referenceRetrieval: ReferenceRetrieval,
                                                clientDetailsRetrieval: ClientDetailsRetrieval,
                                                subscriptionDetailsService: SubscriptionDetailsService)
                                               (val auditingService: AuditingService,
                                                val authService: AuthService,
                                                val appConfig: AppConfig,
                                                val languageUtils: LanguageUtils)
                                               (implicit val ec: ExecutionContext,
                                                mcc: MessagesControllerComponents) extends AuthenticatedController with ImplicitDateFormatter {

  private def form(implicit request: Request[_]): Form[(DateModel, AccountingMethod)] = {
    UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm(_.toLongDate)
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getAgentReference
        maybeProperty <- subscriptionDetailsService.fetchProperty(reference)
        clientDetails <- clientDetailsRetrieval.getClientDetails
      } yield {
        val formData: Map[String, String] = UkPropertyIncomeSourcesForm.createPropertyMapData(
          maybeProperty.flatMap(_.startDate),
          maybeProperty.flatMap(_.accountingMethod)
        )
        val boundForm = form.bind(formData).discardingErrors
        Ok(view(
          boundForm,
          isEditMode = isEditMode, clientDetails
        ))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        form.bindFromRequest().fold(
          formWithErrors => clientDetailsRetrieval.getClientDetails map { clientDetails =>
            BadRequest(view(
              formWithErrors, isEditMode, clientDetails
            ))
          }, {
            case (startDate, accountingMethod) =>
              val propertyModel = PropertyModel(accountingMethod = Some(accountingMethod), startDate = Some(startDate))
              subscriptionDetailsService.saveProperty(reference, propertyModel) map {
                case Right(_) => Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode))
                case Left(_) => throw new InternalServerException("[PropertyIncomeSourcesController][submit] - Could not save property")
              }
          }
        )
      }
  }

  private def view(form: Form[(DateModel, AccountingMethod)], isEditMode: Boolean, clientDetails: ClientDetails)
                  (implicit request: Request[AnyContent]): Html = {
    propertyIncomeSources(
      ukPropertyIncomeSourcesForm = form,
      postAction = routes.PropertyIncomeSourcesController.submit(isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode),
      clientDetails = clientDetails
    )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      routes.PropertyCheckYourAnswersController.show(isEditMode).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }
}