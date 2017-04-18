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

package controllers

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.BaseControllerConfig
import forms.{AccountingMethodForm, IncomeSourceForm, OtherIncomeForm}
import models.OtherIncomeModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import services.KeystoreService
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val logging: Logging
                                     ) extends BaseController {

  def view(otherIncomeForm: Form[OtherIncomeModel], backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      postAction = controllers.routes.OtherIncomeController.submitOtherIncome(editMode = isEditMode),
      backUrl = backUrl,
      isEditMode = isEditMode
    )

  def showOtherIncome(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      for {
        choice <- keystoreService.fetchOtherIncome()
      } yield Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl, isEditMode))
  }

  def defaultRedirections(otherIncomeModel: OtherIncomeModel)(implicit request: Request[_]): Future[Result] =
    otherIncomeModel.choice match {
      case OtherIncomeForm.option_yes =>
        Redirect(controllers.routes.OtherIncomeErrorController.showOtherIncomeError())
      case OtherIncomeForm.option_no =>
        keystoreService.fetchIncomeSource() map {
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
            InternalServerError
        }
    }

  def submitOtherIncome(isEditMode: Boolean): Action[AnyContent] = Authorised.async { implicit user =>
    implicit request =>
      OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(otherIncomeForm = formWithErrors, backUrl = backUrl, isEditMode = isEditMode)),
        choice =>
          keystoreService.fetchOtherIncome().flatMap {
            previousOtherIncome =>
              keystoreService.saveOtherIncome(choice).flatMap { _ =>
                // if it's in update mode and the previous answer is the same as current then return to check your answers page
                if (isEditMode && previousOtherIncome.fold(false)(old => old.equals(choice)))
                  Redirect(controllers.routes.CheckYourAnswersController.show())
                else defaultRedirections(choice)
              }
          }
      )
  }

  lazy val backUrl: String = controllers.routes.IncomeSourceController.showIncomeSource().url

}
