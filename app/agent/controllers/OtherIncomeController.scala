/*
 * Copyright 2017 HM Revenue & Customs
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

package agent.controllers

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.AuthenticatedController
import config.BaseControllerConfig
import forms.{IncomeSourceForm, OtherIncomeForm}
import models.{IncomeSourceModel, OtherIncomeModel}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.{AuthService, KeystoreService}
import uk.gov.hmrc.http.InternalServerException
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val authService: AuthService,
                                      val logging: Logging
                                     ) extends AuthenticatedController {

  def view(otherIncomeForm: Form[OtherIncomeModel], incomeSource: String, isEditMode: Boolean, backUrl: String)(implicit request: Request[_]): Html =
    views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      incomeSource = incomeSource,
      postAction = controllers.routes.OtherIncomeController.submitOtherIncome(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def showOtherIncome(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        optIncomeSource <- keystoreService.fetchIncomeSource()
        choice <- if (optIncomeSource.isDefined) keystoreService.fetchOtherIncome() else Future.successful(None)
      } yield (optIncomeSource, choice) match {
        case (Some(IncomeSourceModel(incomeSource)), _) =>
          Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), incomeSource, isEditMode, backUrl(isEditMode)))
        case _ =>
          Redirect(controllers.routes.IncomeSourceController.showIncomeSource())
      }
  }

  def defaultRedirections(optIncomeSource: Option[IncomeSourceModel], otherIncomeModel: OtherIncomeModel)(implicit request: Request[_]): Future[Result] =
    otherIncomeModel.choice match {
      case OtherIncomeForm.option_yes =>
        Redirect(controllers.routes.OtherIncomeErrorController.showOtherIncomeError())
      case OtherIncomeForm.option_no =>
        optIncomeSource match {
          case Some(incomeSource) => incomeSource.source match {
            case IncomeSourceForm.option_business =>
              Redirect(controllers.business.routes.BusinessAccountingPeriodPriorController.show())
            case IncomeSourceForm.option_property =>
              Redirect(controllers.routes.TermsController.showTerms())
            case IncomeSourceForm.option_both =>
              Redirect(controllers.business.routes.BusinessAccountingPeriodPriorController.show())
          }
          case _ =>
            logging.info("Tried to submit other income when no data found in Keystore for income source")
            new InternalServerException("Other Income, tried to submit with no income source")
        }
    }

  def submitOtherIncome(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchIncomeSource().flatMap {
        case optIncomeSource@Some(IncomeSourceModel(incomeSource)) =>
          OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(view(otherIncomeForm = formWithErrors, incomeSource = incomeSource, isEditMode = isEditMode, backUrl = backUrl(isEditMode)))),
            choice =>
              keystoreService.fetchOtherIncome().flatMap {
                previousOtherIncome =>
                  keystoreService.saveOtherIncome(choice).flatMap { _ =>
                    // if it's in update mode and the previous answer is the same as current then return to check your answers page
                    if (isEditMode && previousOtherIncome.fold(false)(old => old.equals(choice)))
                      Future.successful(Redirect(controllers.routes.CheckYourAnswersController.show()))
                    else defaultRedirections(optIncomeSource, choice)
                  }
              }
          )
        case _ =>
          Future.successful(Redirect(controllers.routes.IncomeSourceController.showIncomeSource()))
      }
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.routes.CheckYourAnswersController.show().url
    else
      controllers.routes.IncomeSourceController.showIncomeSource().url

}
