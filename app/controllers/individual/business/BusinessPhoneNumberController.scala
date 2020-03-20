/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.business

import auth.individual.RegistrationController
import config.AppConfig
import forms.individual.business.BusinessPhoneNumberForm
import javax.inject.{Inject, Singleton}
import models.individual.business.BusinessPhoneNumberModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.AuthService
import services.individual.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessPhoneNumberController @Inject()(val authService: AuthService,
                                              val messagesApi: MessagesApi,
                                              keystoreService: KeystoreService)
                                             (implicit val ec: ExecutionContext, val appConfig: AppConfig) extends RegistrationController {

  def view(businessPhoneNumberForm: Form[BusinessPhoneNumberModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.individual.incometax.business.business_phone_number(
      businessPhoneNumberForm = businessPhoneNumberForm,
      postAction = controllers.individual.business.routes.BusinessPhoneNumberController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchBusinessPhoneNumber() map {
        phoneNumber => Ok(view(BusinessPhoneNumberForm.businessPhoneNumberForm.form.fill(phoneNumber), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessPhoneNumberForm.businessPhoneNumberForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        phoneNumber => {
          keystoreService.saveBusinessPhoneNumber(phoneNumber) map (_ =>
            if (isEditMode)
              Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
            else
              Redirect(controllers.individual.business.routes.BusinessAddressController.show())
            )
        }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    else
      controllers.individual.business.routes.BusinessNameController.show().url

}
