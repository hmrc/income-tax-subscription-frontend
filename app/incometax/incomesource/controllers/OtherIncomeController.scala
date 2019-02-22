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

package incometax.incomesource.controllers

import javax.inject.{Inject, Singleton}

import core.audit.Logging
import core.auth.SignUpController
import core.config.BaseControllerConfig
import core.models.{No, Yes, YesNo}
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.incomesource.forms.OtherIncomeForm
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.cache.client.CacheMap

@Singleton
class OtherIncomeController @Inject()(val baseConfig: BaseControllerConfig,
                                      val messagesApi: MessagesApi,
                                      val keystoreService: KeystoreService,
                                      val logging: Logging,
                                      val authService: AuthService,
                                      val currentTimeService: CurrentTimeService
                                     ) extends SignUpController {

  def view(otherIncomeForm: Form[YesNo], backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    incometax.incomesource.views.html.other_income(
      otherIncomeForm = otherIncomeForm,
      postAction = incometax.incomesource.controllers.routes.OtherIncomeController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cache <- keystoreService.fetchAll()
        optIncomeSource = cache.getIncomeSourceType()
        choice = cache.getOtherIncome()
      } yield optIncomeSource match {
        case Some(incomeSourceType) =>
          Ok(view(OtherIncomeForm.otherIncomeForm.fill(choice), backUrl(incomeSourceType, isEditMode), isEditMode))
        case _ =>
          Redirect(defaultInvalidIncomeSourceAction)
      }
  }

  private def defaultRedirections(cache: CacheMap, choice: YesNo)(implicit request: Request[_]): Result =
    choice match {
      case Yes =>
        Redirect(incometax.incomesource.controllers.routes.OtherIncomeErrorController.show())
      case No =>
        cache.getIncomeSourceType() match {
          case Some(Business) =>
            Redirect(incometax.business.controllers.routes.BusinessNameController.show())
          case Some(Property) =>
            Redirect(incometax.subscription.controllers.routes.TermsController.show())
          case Some(Both) =>
            Redirect(incometax.business.controllers.routes.BusinessNameController.show())
          case _ =>
            Redirect(defaultInvalidIncomeSourceAction)
        }
    }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      OtherIncomeForm.otherIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          for {
            cache <- keystoreService.fetchAll()
            incomeSource = cache.getIncomeSourceType().get
          } yield BadRequest(view(otherIncomeForm = formWithErrors, backUrl = backUrl(incomeSource, isEditMode), isEditMode = isEditMode))
        ,
        choice =>
          for {
            cache <- keystoreService.fetchAll()
            previousOtherIncome = cache.getOtherIncome()
            _ <- keystoreService.saveOtherIncome(choice)
          } yield {
            if (isEditMode && previousOtherIncome.contains(choice))
              Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
            else defaultRedirections(cache, choice)
          }
      )
  }

  def backUrl(incomeSourceType: IncomeSourceType, isEditMode: Boolean): String =
    if (isEditMode)
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    else {
      defaultInvalidIncomeSourceAction
    }

  private def defaultInvalidIncomeSourceAction: String =
    incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
}
