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

import auth.{AuthenticatedController, RegistrationController}
import config.BaseControllerConfig
import forms.BusinessPhoneNumberForm
import models.BusinessPhoneNumberModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.{AuthService, KeystoreService}

import scala.concurrent.Future

@Singleton
class BusinessPhoneNumberController @Inject()(val baseConfig: BaseControllerConfig,
                                              val messagesApi: MessagesApi,
                                              val keystoreService: KeystoreService,
                                              val authService: AuthService
                                             ) extends RegistrationController {

  def view(businessPhoneNumberForm: Form[BusinessPhoneNumberModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.business.business_phone_number(
      businessPhoneNumberForm = businessPhoneNumberForm,
      postAction = controllers.business.routes.BusinessPhoneNumberController.submit(editMode = isEditMode),
      backUrl = backUrl,
      isEditMode
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
              Redirect(controllers.routes.CheckYourAnswersController.show())
            else
              NotImplemented
            )
        }
      )
  }

  lazy val backUrl: String = controllers.business.routes.BusinessNameController.showBusinessName().url

}
