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

package controllers.agent.matching

import config.AppConfig
import controllers.SignUpBaseController
import controllers.agent.actions.{ClientDetailsJourneyRefiner, IdentifierAction}
import forms.agent.ClientDetailsForm.clientDetailsForm
import models.audits.SignupStartedAuditing
import models.requests.agent.IdentifierRequest
import models.usermatching.NotLockedOut
import play.api.mvc._
import services.{AuditingService, SessionClearingService, UserLockoutService}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utilities.UserMatchingSessionUtil.{UserMatchingSessionRequestUtil, UserMatchingSessionResultUtil}
import views.html.agent.matching.ClientDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsController @Inject()(view: ClientDetails,
                                        identify: IdentifierAction,
                                        journeyRefiner: ClientDetailsJourneyRefiner,
                                        lockoutService: UserLockoutService,
                                        auditingService: AuditingService,
                                        sessionClearingService: SessionClearingService)
                                       (implicit cc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig) extends SignUpBaseController {

  def show(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    startAgentSignupAudit(agentReferenceNumber = Some(request.arn))
    handleLockOut {
      Future.successful(Ok(view(
        clientDetailsForm = clientDetailsForm.fill(request.fetchUserDetails),
        postAction = routes.ClientDetailsController.submit(editMode = isEditMode),
        backUrl = backUrl,
        isEditMode = isEditMode
      )))
    }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    val sessionData = request.sessionData
    handleLockOut {
      clientDetailsForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            clientDetailsForm = formWithErrors,
            postAction = routes.ClientDetailsController.submit(editMode = isEditMode),
            backUrl = backUrl,
            isEditMode = isEditMode
          ))),
        clientDetails => {
          sessionClearingService.clearAgentSession(routes.ConfirmClientController.show(), sessionData).map(_.saveUserDetails(clientDetails))
        }
      )
    }
  }

  private def handleLockOut(f: => Future[Result])(implicit request: IdentifierRequest[_]): Future[Result] = {
    lockoutService.getLockoutStatus(request.arn) flatMap {
      case Right(NotLockedOut) => f
      case Right(_) => Future.successful(Redirect(controllers.agent.matching.routes.ClientDetailsLockoutController.show.url))
      case Left(_) => throw new InternalServerException("[ClientDetailsController][handleLockOut] lockout failure")
    }
  }

  private def startAgentSignupAudit(agentReferenceNumber: Option[String])(implicit request: Request[_]): Future[AuditResult] = {
    val auditModel = SignupStartedAuditing.SignupStartedAuditModel(
      agentReferenceNumber = agentReferenceNumber,
      utr = None,
      nino = None
    )
    auditingService.audit(auditModel)
  }

  def backUrl: String = {
    appConfig.govukGuidanceITSASignUpAgentLink
  }

}
