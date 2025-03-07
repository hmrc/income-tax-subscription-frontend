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

package controllers.individual

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.business.CaptureConsentForm
import forms.individual.business.CaptureConsentForm.captureConsentForm
import models.{No, Yes, YesNo}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, GetEligibilityStatusService, MandationStatusService, SessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.html.helper.form
import views.html.individual.CaptureConsent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CaptureConsentController @Inject()(view: CaptureConsent,
                                         identify: IdentifierAction,
                                         journeyRefiner: SignUpJourneyRefiner,
                                         sessionDataService: SessionDataService)
                                        (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends SignUpController {
  def show: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    for {
      captureConsentStatus <- sessionDataService.fetchConsentStatus
    } yield {
      captureConsentStatus match {
        case Left(_) => throw new InternalServerException("[CaptureConsentController][show] - Could not fetch consent status")
        case Right(_) =>
          Ok(view(
            captureConsentForm = CaptureConsentForm.captureConsentForm,
            postAction = controllers.individual.routes.CaptureConsentController.submit(),
            backUrl = "/"
          ))
      }
    }
  }

  def submit(): Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
      captureConsentForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            captureConsentForm = formWithErrors,
            postAction = controllers.individual.routes.CaptureConsentController.submit,
            backUrl = "/"
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
                      Redirect(controllers.individual.routes.CaptureConsentController.show())
                  case No =>
                    Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
                }
            }
          }
      )
  }
}
