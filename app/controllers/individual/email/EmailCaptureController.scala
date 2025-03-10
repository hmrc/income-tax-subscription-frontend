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

package controllers.individual.email

import common.Constants.ITSASessionKeys.FULLNAME
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import forms.individual.email.EmailCaptureForm
import models.audits.BetaContactDetails
import models.requests.individual.SignUpRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditingService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import views.html.individual.email.EmailCapture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailCaptureController @Inject()(auditingService: AuditingService,
                                       identify: IdentifierAction,
                                       journeyRefiner: SignUpJourneyRefiner,
                                       view: EmailCapture)
                                      (implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner) { implicit request =>
    Ok(view(
      emailForm = EmailCaptureForm.form,
      postAction = routes.EmailCaptureController.submit(),
      backUrl = controllers.individual.email.routes.CaptureConsentController.show().url
    ))
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    EmailCaptureForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(
        emailForm = formWithErrors,
        postAction = routes.EmailCaptureController.submit(),
        backUrl = controllers.individual.email.routes.CaptureConsentController.show().url
      ))), { email =>
        auditBetaContactDetails(email) map { _ =>
          Redirect(controllers.individual.routes.WhatYouNeedToDoController.show)
        }
      }
    )
  }

  private def auditBetaContactDetails(email: String)(implicit request: SignUpRequest[_]): Future[AuditResult] = {
    val auditModel = BetaContactDetails(
      emailAddress = email,
      agentReferenceNumber = None,
      fullName = request.session.get(FULLNAME),
      nino = Some(request.nino)
    )

    auditingService.audit(auditModel)
  }

}
