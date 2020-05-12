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

package controllers.agent.business

import auth.agent.{AuthenticatedController, UserMatchingController}
import config.AppConfig
import forms.agent.BusinessNameForm
import javax.inject.{Inject, Singleton}
import models.agent.BusinessNameModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesActionBuilder, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.AuthService
import services.agent.KeystoreService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessNameController @Inject()(val authService: AuthService, keystoreService: KeystoreService)
                                      (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents,
                                       appConfig: AppConfig) extends AuthenticatedController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    views.html.agent.business.business_name(
      businessNameForm = businessNameForm,
      postAction = controllers.agent.business.routes.BusinessNameController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        businessName <- keystoreService.fetchBusinessName()
      } yield Ok(view(
        BusinessNameForm.businessNameForm.form.fill(businessName),
        isEditMode = isEditMode
      ))
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        businessName => {
          keystoreService.saveBusinessName(businessName) map (_ =>
            if (isEditMode)
              Redirect(controllers.agent.routes.CheckYourAnswersController.show())
            else
              Redirect(controllers.agent.business.routes.MatchTaxYearController.show())
            )
        }
      )
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) controllers.agent.routes.CheckYourAnswersController.show().url
    else controllers.agent.routes.IncomeSourceController.show().url
  }
}
