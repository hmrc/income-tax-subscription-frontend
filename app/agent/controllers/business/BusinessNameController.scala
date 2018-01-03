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

package agent.controllers.business

import javax.inject.{Inject, Singleton}

import agent.auth.AuthenticatedController
import core.config.BaseControllerConfig
import agent.forms.BusinessNameForm
import agent.models.BusinessNameModel
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import agent.services.KeystoreService
import core.services.AuthService

import scala.concurrent.Future

@Singleton
class BusinessNameController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService
                                      ) extends AuthenticatedController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    agent.views.html.business.business_name(
      businessNameForm = businessNameForm,
      postAction = agent.controllers.business.routes.BusinessNameController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchBusinessName() map {
        businessName => Ok(view(BusinessNameForm.businessNameForm.form.fill(businessName), isEditMode = isEditMode))
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      BusinessNameForm.businessNameForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, isEditMode = isEditMode))),
        businessName => {
          keystoreService.saveBusinessName(businessName) map (_ =>
            if (isEditMode)
              Redirect(agent.controllers.routes.CheckYourAnswersController.show())
            else
              Redirect(agent.controllers.business.routes.BusinessAccountingMethodController.show())
            )
        }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      agent.controllers.routes.CheckYourAnswersController.show().url
    else
      agent.controllers.business.routes.BusinessAccountingPeriodDateController.show().url

}
