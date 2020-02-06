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

import agent.auth.AuthenticatedController
import agent.models.BusinessNameModel
import agent.services.KeystoreService
import core.config.BaseControllerConfig
import core.services.AuthService
import forms.agent.BusinessNameForm
import incometax.incomesource.services.CurrentTimeService
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class BusinessNameController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       currentTimeService: CurrentTimeService
                                      ) extends AuthenticatedController {

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
