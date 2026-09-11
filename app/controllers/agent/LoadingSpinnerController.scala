/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent

import config.AppConfig
import controllers.LoadingSpinnerBaseController
import controllers.agent.actions.IdentifierAction
import models.agent.JourneyStep.Confirmation
import play.api.mvc.*
import services.SessionDataService
import views.html.LoadingSpinner
import views.html.errors.ServiceError

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LoadingSpinnerController @Inject()(
  view: LoadingSpinner,
  serviceError: ServiceError,
  identify: IdentifierAction,
  appConfig: AppConfig,
  sessionDataService: SessionDataService
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends LoadingSpinnerBaseController (
  isAgent = true,
  view = view,
  serviceError = serviceError,
  appConfig = appConfig,
  sessionDataService = sessionDataService,
  confirmation = Confirmation
) {

  def show: Action[AnyContent] = identify.async { implicit request =>
    super.show(
      sessionData = request.sessionData,
      queryAction = routes.LoadingSpinnerController.query,
      onwardAction = routes.ConfirmationController.show,
      returnAction = routes.GlobalCheckYourAnswersController.show,
      errorAction = routes.GlobalCheckYourAnswersController.submit
    )
  }

  def query: Action[AnyContent] = identify { implicit request =>
    super.query(request.sessionData)
  }
}
