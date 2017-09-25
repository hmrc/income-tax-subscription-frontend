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

import javax.inject.Inject

import auth.RegistrationController
import config.BaseControllerConfig
import forms.BusinessStartDateForm
import models.BusinessStartDateModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.{AuthService, KeystoreService}

import scala.concurrent.Future


class BusinessStartDateController @Inject()(val baseConfig: BaseControllerConfig,
                                            val messagesApi: MessagesApi,
                                            val keystoreService: KeystoreService,
                                            val authService: AuthService
                                           ) extends RegistrationController {

  def view(businessStartDateForm: Form[BusinessStartDateModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html =
    views.html.business.business_start_date(
      businessStartDateForm = businessStartDateForm,
      postAction = controllers.business.routes.BusinessStartDateController.submit(editMode = isEditMode),
      backUrl = backUrl(isEditMode),
      isEditMode
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessStartDate <- keystoreService.fetchBusinessStartDate()
      } yield Ok(view(BusinessStartDateForm.businessStartDateForm.fill(businessStartDate), isEditMode = isEditMode))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessStartDateForm.businessStartDateForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        businessStartDate =>
          keystoreService.saveBusinessStartDate(businessStartDate) map (_ =>
            if (isEditMode)
              Redirect(controllers.routes.CheckYourAnswersController.show())
            else
              Redirect(controllers.business.routes.BusinessAccountingPeriodDateController.show())
            )
      )
  }

  // TODO change the end point for linear journey when the edit page comes into play
  // TODO go back to check your answers for edit journey
  def backUrl(isEditMode: Boolean): String = controllers.business.routes.BusinessAddressController.show().url

}

