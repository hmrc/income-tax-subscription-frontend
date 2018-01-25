/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.controllers

import javax.inject.{Inject, Singleton}

import core.audit.Logging
import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import core.utils.Implicits._
import incometax.incomesource.forms.{IncomeSourceForm, OtherIncomeForm}
import incometax.incomesource.models.{NewIncomeSourceModel, OtherIncomeModel}
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.{Both, Business, Property}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val logging: Logging,
                                      val authService: AuthService,
                                      val currentTimeService: CurrentTimeService
                                     ) extends SignUpController {

  def view(otherIncomeForm: Form[OtherIncomeModel], backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    incometax.incomesource.views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      postAction = incometax.incomesource.controllers.routes.OtherIncomeController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (applicationConfig.newIncomeSourceFlowEnabled) {
        for {
          cache <- keystoreService.fetchAll()
          optIncomeSource = cache.getNewIncomeSource()
          choice = cache.getOtherIncome()
        } yield optIncomeSource match {
          case Some(incomeSource) if incomeSource.getIncomeSourceType.isRight =>
            Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl(incomeSource, isEditMode), isEditMode))
          case _ =>
            Redirect(incometax.incomesource.controllers.routes.WorkForYourselfController.show())
        }
      } else {
        for {
          cache <- keystoreService.fetchAll()
          choice = cache.getOtherIncome()
          optIncomeSource = cache.getIncomeSource()
        } yield optIncomeSource match {
          case Some(incomeSource) if incomeSource.source == IncomeSourceForm.option_property =>
            Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl(isProperty = true, isEditMode), isEditMode))
          case Some(incomeSource) =>
            Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl(isProperty = false, isEditMode), isEditMode))
          case _ => Redirect(incometax.incomesource.controllers.routes.IncomeSourceController.show())
        }
      }
  }

  def defaultRedirections(otherIncomeModel: OtherIncomeModel)(implicit request: Request[_]): Future[Result] =
    otherIncomeModel.choice match {
      case OtherIncomeForm.option_yes =>
        Redirect(incometax.incomesource.controllers.routes.OtherIncomeErrorController.show())
      case OtherIncomeForm.option_no =>
        if (applicationConfig.newIncomeSourceFlowEnabled) {
          for {
            cache <- keystoreService.fetchAll()
            optIncomeSource = cache.getNewIncomeSource()
          }yield optIncomeSource match {
            case Some(incomesource) => incomesource.getIncomeSourceType match {
              case Right(Business) =>
                Redirect(incometax.business.controllers.routes.BusinessNameController.show())
              case Right(Property) =>
                Redirect(incometax.subscription.controllers.routes.TermsController.show())
              case Right(Both) =>
                Redirect(incometax.business.controllers.routes.BusinessNameController.show())
              case _ => Redirect(incometax.incomesource.controllers.routes.WorkForYourselfController.show())
            }
            case _ => Redirect(incometax.incomesource.controllers.routes.WorkForYourselfController.show())
          }
        } else {
          keystoreService.fetchIncomeSource() map {
            case Some(incomeSource) => incomeSource.source match {
              case IncomeSourceForm.option_business =>
                Redirect(incometax.business.controllers.routes.BusinessNameController.show())
              case IncomeSourceForm.option_property =>
                Redirect(incometax.subscription.controllers.routes.TermsController.show())
              case IncomeSourceForm.option_both =>
                Redirect(incometax.business.controllers.routes.BusinessNameController.show())
            }
            case _ =>
              logging.info("Tried to submit other income when no data found in Keystore for income source")
              throw new InternalServerException("Other Income Controller, call to submit with no income source")

          }
        }
    }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          if (applicationConfig.newIncomeSourceFlowEnabled) {
            for {
              cache <- keystoreService.fetchAll()
              incomeSource = cache.getNewIncomeSource().get
            } yield BadRequest(view(otherIncomeForm = formWithErrors, backUrl = backUrl(incomeSource, isEditMode), isEditMode = isEditMode))
          }
          else {
            for {
              cache <- keystoreService.fetchAll()
              isProperty = cache.getIncomeSource().get.source == IncomeSourceForm.option_property
            } yield BadRequest(view(otherIncomeForm = formWithErrors, backUrl = backUrl(isProperty, isEditMode), isEditMode = isEditMode))
          },
        choice =>
          keystoreService.fetchOtherIncome().flatMap {
            previousOtherIncome =>
              keystoreService.saveOtherIncome(choice).flatMap { _ =>
                // if it's in update mode and the previous answer is the same as current then return to check your answers page
                if (isEditMode && previousOtherIncome.contains(choice))
                  Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
                else defaultRedirections(choice)
              }
          }
      )
  }

  def backUrl(isProperty: Boolean, isEditMode: Boolean): String =
    if (isEditMode)
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    else {
      if (isProperty && applicationConfig.taxYearDeferralEnabled && currentTimeService.getTaxYearEndForCurrentDate <= 2018)
        incometax.incomesource.controllers.routes.CannotReportYetController.show().url
      else
        incometax.incomesource.controllers.routes.IncomeSourceController.show().url
    }

  def backUrl(newIncomeSource: NewIncomeSourceModel, isEditMode: Boolean): String =
    if (isEditMode)
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    else {
      if (newIncomeSource.getIncomeSourceType == Right(Property)
        && applicationConfig.taxYearDeferralEnabled
        && currentTimeService.getTaxYearEndForCurrentDate <= 2018)
        incometax.incomesource.controllers.routes.CannotReportYetController.show().url
      else
        incometax.incomesource.controllers.routes.WorkForYourselfController.show().url

    }

}
