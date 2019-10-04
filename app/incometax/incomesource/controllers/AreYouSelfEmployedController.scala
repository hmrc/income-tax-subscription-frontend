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

import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.incomesource.forms.AreYouSelfEmployedForm
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
    incometax.incomesource.views.html.are_you_selfemployed(
      areYouSelfEmployedForm = areYouSelfEmployedForm,
      postAction = incometax.incomesource.controllers.routes.AreYouSelfEmployedController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl(isEditMode)
    )

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        cache <- keystoreService.fetchAll()
      } yield
        if (!cache.getRentUkProperty().needSecondPage)
          Redirect(incometax.incomesource.controllers.routes.RentUkPropertyController.show().url)
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
                case Business | Both if appConfig.eligibilityPagesEnabled =>
                  Redirect(incometax.business.controllers.routes.BusinessNameController.show())
                case Property if appConfig.eligibilityPagesEnabled => {
                  if (appConfig.propertyCashOrAccrualsEnabled)
                    Redirect(incometax.business.controllers.routes.PropertyAccountingMethodController.show())
                  else Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
                }
                case Business | Property | Both =>
                  Redirect(incometax.incomesource.controllers.routes.OtherIncomeController.show())
                case Other =>
                  Redirect(incometax.incomesource.controllers.routes.CannotSignUpController.show())
              }
            cache.getIncomeSourceType() match {
              case Some(`incomeSourceType`) if isEditMode => Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.submit())
              case _ => linearJourney
            }
          }
      )
  }

  def backUrl(isEditMode: Boolean): String =
    if (isEditMode)
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    else
      incometax.incomesource.controllers.routes.RentUkPropertyController.show().url
}
