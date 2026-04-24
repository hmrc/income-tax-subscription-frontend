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

import common.Constants.ITSASessionKeys.JourneyStateKey
import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import models.Status.*
import models.SubmissionStatus
import models.agent.JourneyStep.Confirmation
import play.api.mvc.*
import services.SessionDataService
import views.html.agent.LoadingSpinner
import views.html.errors.ServiceError

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoadingSpinnerController @Inject()(view: LoadingSpinner,
                                         serviceError: ServiceError,
                                         identify: IdentifierAction,
                                         appConfig: AppConfig,
                                         sessionDataService: SessionDataService)
                                        (implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = identify.async { implicit request =>
    request.sessionData.fetchSubmissionStatus match {
      case Some(status@SubmissionStatus(InProgress, _)) if status.hasExpired(appConfig.confirmingSubmissionMaxWaitTimeSeconds) =>
        displayServiceError()
      case Some(SubmissionStatus(status, _)) =>
        status match {
          case InProgress =>
            Future.successful(Ok(view(routes.LoadingSpinnerController.query)))
          case Success =>
            Future.successful(Redirect(routes.ConfirmationController.show).addingToSession(JourneyStateKey -> Confirmation.key))
          case HandledError =>
            Future.successful(Redirect(controllers.errors.routes.ContactHMRCController.show))
          case OtherError =>
            displayServiceError()
        }
      case None => Future.successful(Redirect(routes.GlobalCheckYourAnswersController.show))
    }
  }

  private def displayServiceError()(implicit request: Request[_]): Future[Result] = {
    sessionDataService.deleteSubmissionStatus map { _ =>
      InternalServerError(serviceError(
        postAction = routes.GlobalCheckYourAnswersController.submit,
        isAgent = true
      ))
    }
  }


  def query: Action[AnyContent] = identify { implicit request =>
    request.sessionData.fetchSubmissionStatus match {
      case Some(status@SubmissionStatus(InProgress, _)) if !status.hasExpired(appConfig.confirmingSubmissionMaxWaitTimeSeconds) => NoContent
      case _ => Ok
    }
  }

}