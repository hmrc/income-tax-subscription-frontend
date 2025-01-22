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
import config.featureswitch.FeatureSwitch.StartDateBeforeLimit
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.UkPropertyIncomeSourcesForm
import models.{AccountingMethod, DateModel, No, Yes}
import play.api.data.Form
import play.api.mvc._
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
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
                                               (val appConfig: AppConfig)
                                               (implicit cc: MessagesControllerComponents, ec: ExecutionContext)
  extends SignUpBaseController with FeatureSwitching {

  def show(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    subscriptionDetailsService.fetchProperty(request.reference) map { maybeProperty =>
      val formData: Map[String, String] = UkPropertyIncomeSourcesForm.createPropertyMapData(maybeProperty)
      val form: Form[_] = ukPropertyIncomeSourceForm.fold(identity, identity)
      Ok(view(
        ukPropertyIncomeSourcesForm = form.bind(formData).discardingErrors,
        postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
        backUrl = backUrl(isEditMode, isGlobalEdit),
        clientDetails = request.clientDetails
      ))
    }
  }

  def submit(isEditMode: Boolean, isGlobalEdit: Boolean): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    ukPropertyIncomeSourceForm match {
      case Left(form) =>
        form.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(
            ukPropertyIncomeSourcesForm = formWithErrors,
            postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(isEditMode, isGlobalEdit),
            clientDetails = request.clientDetails
          ))),
          {
            case (startDate, accountingMethod) =>
              saveDataAndContinue(
                reference = request.reference,
                maybeStartDate = Some(startDate),
                maybeStartDateBeforeLimit = None,
                accountingMethod = accountingMethod,
                isEditMode = isEditMode,
                isGlobalEdit = isGlobalEdit
              )
          }
        )
      case Right(form) =>
        form.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(
            ukPropertyIncomeSourcesForm = formWithErrors,
            postAction = routes.PropertyIncomeSourcesController.submit(editMode = isEditMode, isGlobalEdit = isGlobalEdit),
            backUrl = backUrl(isEditMode, isGlobalEdit),
            clientDetails = request.clientDetails
          ))),
          {
            case (startDateBeforeLimit, accountingMethod) =>
              saveDataAndContinue(
                reference = request.reference,
                maybeStartDate = None,
                maybeStartDateBeforeLimit = startDateBeforeLimit match {
                  case Yes => Some(true)
                  case No => Some(false)
                },
                accountingMethod = accountingMethod,
                isEditMode = isEditMode,
                isGlobalEdit = isGlobalEdit
              )
          }
        )
    }
  }

  private def saveDataAndContinue(reference: String,
                                  maybeStartDate: Option[DateModel],
                                  maybeStartDateBeforeLimit: Option[Boolean],
                                  accountingMethod: AccountingMethod,
                                  isEditMode: Boolean,
                                  isGlobalEdit: Boolean)(implicit hc: HeaderCarrier): Future[Result] = {

    subscriptionDetailsService.saveStreamlineProperty(
      reference = reference,
      maybeStartDate = maybeStartDate,
      maybeStartDateBeforeLimit = maybeStartDateBeforeLimit,
      accountingMethod = accountingMethod
    ) map {
      case Right(_) =>
        if (maybeStartDateBeforeLimit.contains(false)) {
          Redirect(routes.PropertyStartDateController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
        } else {
          Redirect(routes.PropertyCheckYourAnswersController.show(editMode = isEditMode, isGlobalEdit = isGlobalEdit))
        }
      case Left(_) => throw new InternalServerException("[PropertyIncomeSourcesController][saveDataAndContinue] - Could not save property income source")
    }

  }

  private def ukPropertyIncomeSourceForm(implicit request: Request[_]) = {
    import implicitDateFormatter.LongDate

    if (isEnabled(StartDateBeforeLimit)) {
      Right(UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesFormNoDate)
    } else {
      Left(UkPropertyIncomeSourcesForm.ukPropertyIncomeSourcesForm(_.toLongDate))
    }
  }

  private def backUrl(isEditMode: Boolean, isGlobalEdit: Boolean): String = {
    if (isEditMode || isGlobalEdit) {
      routes.PropertyCheckYourAnswersController.show(isEditMode, isGlobalEdit).url
    } else {
      controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
  }

}