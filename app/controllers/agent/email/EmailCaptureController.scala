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
import forms.agent.email.EmailCaptureForm
import models.audits.BetaContactDetails
import models.requests.agent.ConfirmedClientRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditingService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.agent.email.EmailCapture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailCaptureController @Inject()(identify: IdentifierAction,
                                       journeyRefiner: ConfirmedClientJourneyRefiner,
                                       view: EmailCapture,
                                       auditingService: AuditingService)
                                      (implicit mcc: MessagesControllerComponents,
                                       ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      emailForm = EmailCaptureForm.form,
      postAction = routes.EmailCaptureController.submit(),
      backUrl = controllers.agent.email.routes.CaptureConsentController.show().url
    ))
  }


  def submit: Action[AnyContent] = (identify andThen journeyRefiner) async { implicit request =>
    EmailCaptureForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(
        emailForm = formWithErrors,
        postAction = routes.EmailCaptureController.submit(),
        backUrl = controllers.agent.email.routes.CaptureConsentController.show().url
      ))), {
        email =>
          auditBetaContactDetails(email) map { _ =>
            Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
          }
      }
    )
  }

  private def auditBetaContactDetails(email: String)(implicit request: ConfirmedClientRequest[_]): Future[AuditResult] = {
    val auditModel = BetaContactDetails(
      emailAddress = email,
      agentReferenceNumber = Some(request.arn),
      fullName = None,
      nino = None)

    auditingService.audit(auditModel)
  }
}
