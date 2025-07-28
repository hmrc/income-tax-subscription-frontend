/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.agent.PropertyStartDateForm
import forms.agent.PropertyStartDateForm.propertyStartDateForm
import models.DateModel
import play.api.data.Form
import play.api.mvc._
import _root_.config.featureswitch.FeatureSwitch.RemoveAccountingMethod
import _root_.config.featureswitch.FeatureSwitching
import _root_.config.AppConfig
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatter
import views.html.agent.tasklist.ukproperty.PropertyStartDate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyStartDateController @Inject()(identify: IdentifierAction,
                                            journeyRefiner: ConfirmedClientJourneyRefiner,
                                            subscriptionDetailsService: SubscriptionDetailsService,
                                            view: PropertyStartDate,
                                            implicitDateFormatter: ImplicitDateFormatter)
                                           (val appConfig: AppConfig)
                                           (implicit mcc: MessagesControllerComponents,
                                            ec: ExecutionContext) extends SignUpBaseController with FeatureSwitching {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      startDate <- subscriptionDetailsService.fetchPropertyStartDate(request.reference)
    } yield {
      Ok(view(
        propertyStartDateForm = form.fill(startDate),
        postAction = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          propertyStartDateForm = formWithErrors,
          postAction = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      startDate =>
        subscriptionDetailsService.savePropertyStartDate(request.reference, startDate) map {
          case Right(_) => Redirect(routes.PropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
          case Left(_) => throw new InternalServerException("[PropertyStartDateController][submit] - Could not save start date")
        }
    )
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEnabled(RemoveAccountingMethod)) {
      routes.PropertyStartDateBeforeLimitController.show(isEditMode, isGlobalEdit).url
    } else {
      routes.PropertyIncomeSourcesController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit).url
    }
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    import implicitDateFormatter.LongDate

    propertyStartDateForm(
      PropertyStartDateForm.minStartDate,
      PropertyStartDateForm.maxStartDate,
      _.toLongDate
    )
  }

}
