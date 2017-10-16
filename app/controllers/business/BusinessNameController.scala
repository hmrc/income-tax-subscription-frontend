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

package controllers.business

import javax.inject.{Inject, Singleton}

import core.auth.{Registration, SignUpController}
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import forms.BusinessNameForm
import models.{BusinessNameModel, OtherIncomeModel}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessNameController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService
                                      ) extends SignUpController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Html] =
    backUrl(isEditMode).map { backUrl =>
      views.html.business.business_name(
        businessNameForm = businessNameForm,
        postAction = controllers.business.routes.BusinessNameController.submit(editMode = isEditMode),
        isRegistration = request.isInState(Registration),
        isEditMode,
        backUrl = backUrl
      )
    }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessName <- keystoreService.fetchBusinessName()
        view <- view(BusinessNameForm.businessNameForm.form.fill(businessName), isEditMode = isEditMode)
      } yield Ok(view)
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors =>
          view(formWithErrors, isEditMode = isEditMode).map(BadRequest(_)),
        businessName =>
          keystoreService.saveBusinessName(businessName) map (_ =>
            if (isEditMode)
              Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
            else if (request.isInState(Registration))
              Redirect(controllers.business.routes.BusinessPhoneNumberController.show())
            else
              Redirect(controllers.business.routes.BusinessAccountingPeriodPriorController.show())
            )
      )
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[_]): Future[String] = {
    import forms.OtherIncomeForm._
    if (isEditMode)
      Future.successful(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
    else
      keystoreService.fetchOtherIncome().map {
        case Some(OtherIncomeModel(`option_yes`)) => controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url
        case _ => controllers.routes.OtherIncomeController.showOtherIncome().url
      }
  }

}
