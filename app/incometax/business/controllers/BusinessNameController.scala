/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.business.controllers

import core.auth.{Registration, SignUpController}
import core.config.BaseControllerConfig
import core.models.Yes
import core.services.{AuthService, KeystoreService}
import incometax.business.forms.BusinessNameForm
import incometax.business.models.BusinessNameModel
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
                                       val authService: AuthService
                                      ) extends SignUpController {

  def view(businessNameForm: Form[BusinessNameModel], isEditMode: Boolean)(implicit request: Request[AnyContent]): Future[Html] =
    backUrl(isEditMode).map { backUrl =>
      incometax.business.views.html.business_name(
        businessNameForm = businessNameForm,
        postAction = incometax.business.controllers.routes.BusinessNameController.submit(editMode = isEditMode),
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
              Redirect(incometax.business.controllers.routes.BusinessPhoneNumberController.show())
            else
              Redirect(incometax.business.controllers.routes.MatchTaxYearController.show())
            )
      )
  }

  def backUrl(isEditMode: Boolean)(implicit request: Request[_]): Future[String] = {
    if (isEditMode)
      Future.successful(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
    else if (applicationConfig.eligibilityPagesEnabled) {
      Future.successful(incometax.incomesource.controllers.routes.WorkForYourselfController.show().url)
    } else {
      keystoreService.fetchOtherIncome().map {
        case Some(Yes) =>
          incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url
        case _ =>
          incometax.incomesource.controllers.routes.OtherIncomeController.show().url
      }
    }
  }

}
