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

package controllers

import config.AppConfig
import controllers.SignUpBaseController
import models.Status.*
import models.{JourneyStep, SessionData, SubmissionStatus}
import play.api.mvc.*
import services.SessionDataService
import views.html.LoadingSpinner
import views.html.errors.ServiceError

import scala.concurrent.{ExecutionContext, Future}

abstract class LoadingSpinnerBaseController (
  isAgent: Boolean,
  view: LoadingSpinner,
  serviceError: ServiceError,
  appConfig: AppConfig,
  sessionDataService: SessionDataService,
  confirmation: JourneyStep
)(implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  protected final def show(
    sessionData: SessionData,
    queryAction: Call,
    onwardAction: Call,
    returnAction: Call,
    errorAction: Call
  )(implicit request: Request[_]): Future[Result] = {
    sessionData.fetchSubmissionStatus match {
      case Some(status@SubmissionStatus(InProgress, _))
        if status.hasExpired(appConfig.confirmingSubmissionMaxWaitTimeSeconds) =>
          displayServiceError(errorAction)
      case Some(SubmissionStatus(status, _)) =>
        status match {
          case InProgress =>
            Future.successful(Ok(view(isAgent, queryAction)))
          case Success =>
            sessionDataService.saveJourneyStep(confirmation).map { _ =>
              Redirect(onwardAction)
            }
          case HandledError =>
            Future.successful(Redirect(controllers.errors.routes.ContactHMRCController.show))
          case OtherError =>
            displayServiceError(errorAction)
        }
      case None => Future.successful(Redirect(returnAction))
    }
  }

  private def displayServiceError(errorAction: Call)(implicit request: Request[_]): Future[Result] = {
    sessionDataService.deleteSubmissionStatus map { _ =>
      InternalServerError(serviceError(
        postAction = errorAction,
        isAgent = isAgent
      ))
    }
  }

  protected final def query(
    sessionData: SessionData
  ): Result = {
    sessionData.fetchSubmissionStatus match {
      case Some(status @ SubmissionStatus(InProgress, _))
        if !status.hasExpired(appConfig.confirmingSubmissionMaxWaitTimeSeconds) =>
          NoContent
      case _ =>
        Ok
    }
  }
}