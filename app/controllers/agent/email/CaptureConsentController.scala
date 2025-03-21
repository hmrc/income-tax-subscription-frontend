/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.agent.email

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import forms.agent.email.CaptureConsentForm.captureConsentForm
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionDataService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.email.CaptureConsent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CaptureConsentController @Inject()(view: CaptureConsent,
                                         identify: IdentifierAction,
                                         journeyRefiner: ConfirmedClientJourneyRefiner,
                                         sessionDataService: SessionDataService)
                                        (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    for {
      captureConsentStatus <- sessionDataService.fetchConsentStatus
    } yield {
      captureConsentStatus match {
        case Left(_) => throw new InternalServerException("[CaptureConsentController][show] - Could not fetch consent status")
        case Right(maybeYesNo) =>
          Ok(view(
            captureConsentForm = captureConsentForm.fill(maybeYesNo),
            postAction = controllers.agent.email.routes.CaptureConsentController.submit(),
            clientName = request.clientDetails.name,
            clientNino = request.clientDetails.formattedNino,
            backUrl = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
          ))
      }
    }
  }

  def submit(): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    captureConsentForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(
          captureConsentForm = formWithErrors,
          postAction = controllers.agent.email.routes.CaptureConsentController.submit(),
          clientName = request.clientDetails.name,
          clientNino = request.clientDetails.formattedNino,
          backUrl = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
        ))),
      yesNo =>
        for {
          captureConsentStatus <- sessionDataService.saveConsentStatus(yesNo)
        } yield {

          captureConsentStatus match {
            case Left(_) =>
              throw new InternalServerException("[CaptureConsentController][submit] - Could not save capture consent answer")
            case Right(_) =>
              yesNo match {
                case Yes =>
                  Redirect(controllers.agent.email.routes.EmailCaptureController.show())
                case No =>
                  Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
              }
          }
        }
    )
  }

}
