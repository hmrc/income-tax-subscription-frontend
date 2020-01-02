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

import core.auth.RegistrationController
import core.config.BaseControllerConfig
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.BusinessStartDateForm
import incometax.business.models.BusinessStartDateModel
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future


class BusinessStartDateController @Inject()(val baseConfig: BaseControllerConfig,
                                            val messagesApi: MessagesApi,
                                            val keystoreService: KeystoreService,
                                            val authService: AuthService
                                           ) extends RegistrationController {

  def view(businessStartDateForm: Form[BusinessStartDateModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Html =
    incometax.business.views.html.business_start_date(
      businessStartDateForm = businessStartDateForm,
      postAction = controllers.individual.business.routes.BusinessStartDateController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
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
              Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.show())
            else
              Redirect(controllers.individual.business.routes.MatchTaxYearController.show())
            )
      )
  }

  // TODO change the end point for linear journey when the edit page comes into play
  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    else
      controllers.individual.business.routes.BusinessAddressController.show().url

}

