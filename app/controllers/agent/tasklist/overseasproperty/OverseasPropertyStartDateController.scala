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

package controllers.agent.tasklist.overseasproperty

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.OverseasPropertyStartDateForm
import forms.agent.OverseasPropertyStartDateForm._
import models.DateModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatter
import views.html.agent.tasklist.overseasproperty.OverseasPropertyStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasPropertyStartDateController @Inject()(identify: IdentifierAction,
                                                    journeyRefiner: ConfirmedClientJourneyRefiner,
                                                    subscriptionDetailsService: SubscriptionDetailsService,
                                                    view: OverseasPropertyStartDate,
                                                    implicitDateFormatter: ImplicitDateFormatter)
                                                   (val appConfig: AppConfig)
                                                   (implicit val ec: ExecutionContext,
                                                    mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      startDate <- subscriptionDetailsService.fetchForeignPropertyStartDate(request.reference)
    } yield {
      Ok(view(
        overseasPropertyStartDateForm = form.fill(startDate),
        postAction = routes.OverseasPropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          overseasPropertyStartDateForm = formWithErrors,
          postAction = routes.OverseasPropertyStartDateController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
          backUrl = backUrl(isEditMode, isGlobalEdit),
          clientDetails = request.clientDetails
        ))),
      startDate =>
        subscriptionDetailsService.saveForeignPropertyStartDate(request.reference, startDate) map {
          case Right(_) => Redirect(routes.OverseasPropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
          case Left(_) => throw new InternalServerException("[OverseasPropertyStartDateController][submit] - Could not save start date")
        }
    )
  }

  def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    routes.OverseasPropertyStartDateBeforeLimitController.show(isEditMode, isGlobalEdit).url
  }

  private def form(implicit request: Request[_]): Form[DateModel] = {
    import implicitDateFormatter.LongDate

    overseasPropertyStartDateForm(
      OverseasPropertyStartDateForm.minStartDate,
      OverseasPropertyStartDateForm.maxStartDate,
      _.toLongDate
    )
  }
}
