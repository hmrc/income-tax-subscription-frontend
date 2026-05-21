/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.iv

import common.Constants.ITSASessionKeys
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.IVOutcomeFailureAuditing.IVOutcomeFailureAuditModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import controllers.SignUpBaseController
import controllers.individual.actions.BasicIdentifierAction
import services.AuditingService
import views.html.individual.iv.IVFailure

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IVFailureController @Inject()(auditingService: AuditingService,
                                    identify: BasicIdentifierAction,
                                    ivFailure: IVFailure)
                                   (implicit val ec: ExecutionContext,
                                    mcc: MessagesControllerComponents) extends SignUpBaseController {

  def view(implicit request: Request[_]): Html = ivFailure()

  def failure: Action[AnyContent] = identify.async { implicit request =>
    if (request.session.get(ITSASessionKeys.IdentityVerificationFlag).nonEmpty) {
      request.getQueryString("journeyId").foreach(id => auditingService.audit(IVOutcomeFailureAuditModel(id)))
      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = None,
        utr = None,
        nino = None,
        eligibility = "ineligible",
        failureReason = Some("iv-failure")
      ))
      Future.successful(Ok(view).removingFromSession(ITSASessionKeys.IdentityVerificationFlag))
    } else {
      Future.successful(Ok(view))
    }
  }

}
