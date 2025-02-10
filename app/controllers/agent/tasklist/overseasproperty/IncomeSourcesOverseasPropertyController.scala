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

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.IncomeSourcesOverseasPropertyForm
import forms.agent.IncomeSourcesOverseasPropertyForm.overseasPropertyIncomeSourcesFormNoDate
import models.{No, Yes}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.tasklist.overseasproperty.IncomeSourcesOverseasProperty

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesOverseasPropertyController @Inject()(identify: IdentifierAction,
                                                        journeyRefiner: ConfirmedClientJourneyRefiner,
                                                        subscriptionDetailsService: SubscriptionDetailsService,
                                                        view: IncomeSourcesOverseasProperty)
                                                       (val appConfig: AppConfig)
                                                       (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchOverseasProperty(request.reference) map { maybeOverseasProperty =>
      val formData: Map[String, String] = IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(maybeOverseasProperty)
      val form: Form[_] = overseasPropertyIncomeSourcesFormNoDate
      Ok(view(
        incomeSourcesOverseasPropertyForm = form.bind(formData).discardingErrors,
        postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
      overseasPropertyIncomeSourcesFormNoDate.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
                  incomeSourcesOverseasPropertyForm = formWithErrors,
                  postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
                  backUrl = backUrl(isEditMode, isGlobalEdit),
                  clientDetails = request.clientDetails
                ))),
          { case (startDateBeforeLimit, accountingMethod) =>
            subscriptionDetailsService.saveStreamlineForeignProperty(
                reference = request.reference,
                maybeStartDate = None,
                maybeStartDateBeforeLimit = startDateBeforeLimit match {
                  case Yes => Some(true)
                  case No  => Some(false)
                },
                accountingMethod = accountingMethod
              )
              .map {
                case Right(_) =>
                  if (startDateBeforeLimit == No) {
                    Redirect(controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
                  } else {
                    Redirect(controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
                  }
                case Left(_) => throw new InternalServerException("[IncomeSourcesOverseasPropertyController][saveDataAndContinue] - Could not save foreign property income source")
              }
          }
        )
    }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}
