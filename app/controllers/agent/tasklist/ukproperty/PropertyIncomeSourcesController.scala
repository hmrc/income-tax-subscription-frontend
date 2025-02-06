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

import config.AppConfig
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.UkPropertyIncomeSourcesForm
import forms.agent.UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm
import models.{No, Yes}
import play.api.data.Form
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.ukproperty.PropertyIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyIncomeSourcesController @Inject()(identify: IdentifierAction,
                                                journeyRefiner: ConfirmedClientJourneyRefiner,
                                                subscriptionDetailsService: SubscriptionDetailsService,
                                                view: PropertyIncomeSources)
                                               (val appConfig: AppConfig)
                                               (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController with FeatureSwitching {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchProperty(request.reference) map { maybeProperty =>
      val formData: Map[String, String] = UkPropertyIncomeSourcesForm.createPropertyMapData(maybeProperty)
      Ok(view(
        ukPropertyIncomeSourcesForm = ukPropertyIncomeSourcesForm.bind(formData).discardingErrors,
        postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    ukPropertyIncomeSourcesForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          ukPropertyIncomeSourcesForm = formWithErrors,
          postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      { case (startDateBeforeLimit, accountingMethod) =>
        subscriptionDetailsService.saveStreamlineProperty(
          reference = request.reference,
          maybeStartDate = None,
          maybeStartDateBeforeLimit = startDateBeforeLimit match {
            case Yes => Some(true)
            case No => Some(false)
          },
          accountingMethod = accountingMethod
        ) map {
          case Right(_) =>
            if (startDateBeforeLimit == No) {
              Redirect(routes.PropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            } else {
              Redirect(routes.PropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
            }
          case Left(_) => throw new InternalServerException("[PropertyIncomeSourcesController][saveDataAndContinue] - Could not save property income source")
        }
      }
    )
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}