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

package controllers.individual.incomesource

import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import forms.individual.incomesource.AreYouSelfEmployedForm
import incometax.incomesource.models.AreYouSelfEmployedModel
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models._
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class AreYouSelfEmployedController @Inject()(val baseConfig: BaseControllerConfig,
                                             val messagesApi: MessagesApi,
                                             val keystoreService: KeystoreService,
                                             val authService: AuthService,
                                             val appConfig: AppConfig,
                                             val currentTimeService: CurrentTimeService
                                            ) extends SignUpController {

  def view(areYouSelfEmployedForm: Form[AreYouSelfEmployedModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.individual.incometax.incomesource.are_you_selfemployed(
      areYouSelfEmployedForm = areYouSelfEmployedForm,
      postAction = controllers.individual.incomesource.routes.AreYouSelfEmployedController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cache <- keystoreService.fetchAll()
      } yield
        if (!cache.getRentUkProperty().needSecondPage)
          Redirect(controllers.individual.incomesource.routes.RentUkPropertyController.show().url)
        else {
          val cachedModel = cache.getAreYouSelfEmployed()
          Ok(view(areYouSelfEmployedForm = AreYouSelfEmployedForm.areYouSelfEmployedForm.fill(cachedModel), isEditMode = isEditMode))
        }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      AreYouSelfEmployedForm.areYouSelfEmployedForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(areYouSelfEmployedForm = formWithErrors, isEditMode = isEditMode))),
        areYouSelfEmployed =>
          for {
            cache <- keystoreService.fetchAll()
            _ <- keystoreService.saveAreYouSelfEmployed(areYouSelfEmployed)
            rentUkProperty = cache.getRentUkProperty().get
          } yield {
            val incomeSourceType = IncomeSourceType(rentUkProperty, Some(areYouSelfEmployed))
            lazy val linearJourney: Result =
              incomeSourceType match {
                case Some(Business | Both) =>
                  Redirect(controllers.individual.business.routes.BusinessNameController.show())
                case Some(Property) =>
                    Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
                case _ =>
                  Redirect(controllers.individual.incomesource.routes.CannotSignUpController.show())
              }
            cache.getIncomeSourceType() match {
              case `incomeSourceType` if isEditMode => Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.submit())
              case _ => linearJourney
            }
          }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    else
      controllers.individual.incomesource.routes.RentUkPropertyController.show().url
}
