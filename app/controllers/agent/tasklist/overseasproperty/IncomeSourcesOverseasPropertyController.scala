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

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.IncomeSourcesOverseasPropertyForm
import models.common.OverseasPropertyModel
import models.{AccountingMethod, DateModel}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatter
import views.html.agent.tasklist.overseasproperty.IncomeSourcesOverseasProperty

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesOverseasPropertyController @Inject()(identify: IdentifierAction,
                                                        implicitDateFormatter: ImplicitDateFormatter,
                                                        journeyRefiner: ConfirmedClientJourneyRefiner,
                                                        subscriptionDetailsService: SubscriptionDetailsService,
                                                        view: IncomeSourcesOverseasProperty)
                                                       (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchOverseasProperty(request.reference) map { maybeOverseasProperty =>
      val formData: Map[String, String] = IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(
        maybeOverseasProperty.flatMap(_.startDate),
        maybeOverseasProperty.flatMap(_.accountingMethod)
      )
      val boundForm = form.bind(formData).discardingErrors
      Ok(view(
        incomeSourcesOverseasPropertyForm = boundForm,
        postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          incomeSourcesOverseasPropertyForm = formWithErrors,
          postAction = routes.IncomeSourcesOverseasPropertyController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      {
        case (startDate, accountingMethod) =>
          val overseasPropertyModel = OverseasPropertyModel(accountingMethod = Some(accountingMethod), startDate = Some(startDate))
          subscriptionDetailsService.saveOverseasProperty(request.reference, overseasPropertyModel) map {
            case Right(_) => Redirect(routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
            case Left(_) => throw new InternalServerException("[IncomeSourcesOverseasPropertyController][submit] - Could not save overseas property")
          }
      }
    )
  }

  private def form(implicit request: Request[_]): Form[(DateModel, AccountingMethod)] = {
    import implicitDateFormatter.LongDate
    IncomeSourcesOverseasPropertyForm.incomeSourcesOverseasPropertyForm(_.toLongDate)
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.OverseasPropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}
