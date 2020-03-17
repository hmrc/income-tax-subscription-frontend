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

import core.auth.{Registration, SignUpController}
import core.config.AppConfig
import forms.individual.business.BusinessNameForm
import javax.inject.{Inject, Singleton}
import models.individual.business.BusinessNameModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.AuthService
import services.individual.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessNameController @Inject()(val authService: AuthService,
                                       val messagesApi: MessagesApi,
                                       keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, appConfig: AppConfig) extends SignUpController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html = {
    views.html.individual.incometax.business.business_name(
      businessNameForm = businessNameForm,
      postAction = controllers.individual.business.routes.BusinessNameController.submit(editMode = isEditMode),
      isRegistration = request.isInState(Registration),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessName <- keystoreService.fetchBusinessName()
      } yield Ok(view(BusinessNameForm.businessNameForm.form.fill(businessName), isEditMode = isEditMode))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        businessName =>
          keystoreService.saveBusinessName(businessName) map (_ =>
            if (isEditMode)
              Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
            else if (request.isInState(Registration))
              Redirect(controllers.individual.business.routes.BusinessPhoneNumberController.show())
            else
              Redirect(controllers.individual.business.routes.MatchTaxYearController.show())
            )
      )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } else {
      controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
    }
  }

}
