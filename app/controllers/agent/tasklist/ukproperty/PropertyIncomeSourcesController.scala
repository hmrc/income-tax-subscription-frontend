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

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.UkPropertyIncomeSourcesForm
import models.common.PropertyModel
import models.{AccountingMethod, DateModel}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatter
import views.html.agent.tasklist.ukproperty.PropertyIncomeSources

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyIncomeSourcesController @Inject()(identify: IdentifierAction,
                                                implicitDateFormatter: ImplicitDateFormatter,
                                                journeyRefiner: ConfirmedClientJourneyRefiner,
                                                subscriptionDetailsService: SubscriptionDetailsService,
                                                view: PropertyIncomeSources)
                                               (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchProperty(request.reference) map { maybeProperty =>
      val formData: Map[String, String] = UkPropertyIncomeSourcesForm.createPropertyMapData(
        maybeProperty.flatMap(_.startDate),
        maybeProperty.flatMap(_.accountingMethod)
      )
      val boundForm = form.bind(formData).discardingErrors
      Ok(view(
        ukPropertyIncomeSourcesForm = boundForm,
        postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          ukPropertyIncomeSourcesForm = formWithErrors,
          postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      {
        case (startDate, accountingMethod) =>
          val propertyModel = PropertyModel(accountingMethod = Some(accountingMethod), startDate = Some(startDate))
          subscriptionDetailsService.saveProperty(request.reference, propertyModel) map {
            case Right(_) => Redirect(routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit))
            case Left(_) => throw new InternalServerException("[PropertyIncomeSourcesController][submit] - Could not save property")
          }
      }
    )
  }

  private def form(implicit request: Request[_]): Form[(DateModel, AccountingMethod)] = {
    import implicitDateFormatter.LongDate
    UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm(_.toLongDate)
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}