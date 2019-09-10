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
import core.auth.SignUpController
import core.config.{AppConfig, BaseControllerConfig}
import core.services.CacheUtil._
import core.services.{AuthService, KeystoreService}
import incometax.incomesource.forms.WorkForYourselfForm
import incometax.incomesource.models.WorkForYourselfModel
import incometax.incomesource.services.CurrentTimeService
import incometax.subscription.models._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.Html

import scala.concurrent.Future

@Singleton
class WorkForYourselfController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val keystoreService: KeystoreService,
                                          val authService: AuthService,
                                          val appConfig: AppConfig,
                                          val currentTimeService: CurrentTimeService
                                         ) extends SignUpController {

  def view(workForYourselfForm: Form[WorkForYourselfModel], isEditMode: Boolean)(implicit request: Request[_]): Html =
    incometax.incomesource.views.html.work_for_yourself(
      workForYourselfForm = workForYourselfForm,
      postAction = incometax.incomesource.controllers.routes.WorkForYourselfController.submit(editMode = isEditMode),
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
          val cachedModel = cache.getWorkForYourself()
          Ok(view(workForYourselfForm = WorkForYourselfForm.workForYourselfForm.fill(cachedModel), isEditMode = isEditMode))
        }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      WorkForYourselfForm.workForYourselfForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(workForYourselfForm = formWithErrors, isEditMode = isEditMode))),
        workForYourself =>
          for {
            cache <- keystoreService.fetchAll()
            _ <- keystoreService.saveWorkForYourself(workForYourself)
            rentUkProperty = cache.getRentUkProperty().get
          } yield {
            val incomeSourceType = IncomeSourceType(rentUkProperty, Some(workForYourself))
            lazy val linearJourney: Result =
              incomeSourceType match {
                case Business | Property | Both if appConfig.eligibilityPagesEnabled =>
                  Redirect(incometax.subscription.controllers.routes.CheckYourAnswersController.show())
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
