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

import auth.agent.AgentSignUp
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{FailedClientMatching, JourneyStateKey}
import config.AppConfig
import config.featureswitch.FeatureSwitch.WhenDoYouWantToStartPage
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import controllers.utils.ReferenceRetrieval
import models.EligibilityStatus
import models.agent.JourneyStep
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.requests.agent.IdentifierRequest
import models.status.MandationStatus.{Mandated, Voluntary}
import play.api.mvc.*
import services.*
import services.PrePopDataService.PrePopResult
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utilities.UserMatchingSessionUtil.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedClientResolver @Inject()(identify: IdentifierAction,
                                        getEligibilityStatusService: GetEligibilityStatusService,
                                        mandationStatusService: MandationStatusService,
                                        throttlingService: ThrottlingService,
                                        referenceRetrieval: ReferenceRetrieval,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        prePopDataService: PrePopDataService,
                                        auditingService: AuditingService,
                                        ninoService: NinoService,
                                        utrService: UTRService,
                                        val appConfig: AppConfig)
                                       (implicit ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends SignUpBaseController with FeatureSwitching {

  def resolve: Action[AnyContent] = identify.async { implicit request =>
    handleThrottleCheck {
      ninoService.getNino(request.sessionData) flatMap { nino =>
        handleEligibility(nino)(
          goToCannotTakePart,
          handleEligibleUser(nino, _)
        )
      }
    }
  }

  private def handleThrottleCheck(f: => Future[Result])(implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    throttlingService.throttled(AgentStartOfJourneyThrottle, request.sessionData) {
      f
    }
  }

  private def handleEligibility(nino: String)
                               (ineligible: => Future[Result], eligible: EligibilityStatus => Future[Result])
                               (implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    utrService.getUTR(request.sessionData) flatMap { utr =>
      getEligibilityStatusService.getEligibilityStatus(request.sessionData) flatMap {
        case EligibilityStatus(false, false, _) =>
          auditIneligibleResult(nino, utr, request.arn) flatMap { _ =>
            ineligible
          }
        case eligibilityStatus =>
          auditEligibleResult(nino, utr, request.arn, eligibilityStatus) flatMap { _ =>
            eligible(eligibilityStatus)
          }
      }
    }
  }

  private def handleEligibleUser(nino: String, eligibilityStatus: EligibilityStatus)(implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    referenceRetrieval.getReference(Some(request.arn), request.sessionData) flatMap { reference =>
      handlePrePop(reference, nino) {
        goToSignUpClient(reference, eligibilityStatus).map(_.addingToSession(JourneyStateKey -> AgentSignUp.name))
      }
    }
  }

  private def handlePrePop(reference: String, nino: String)(result: => Future[Result])(implicit request: IdentifierRequest[_]): Future[Result] = {
    prePopDataService.prePopIncomeSources(reference, nino) flatMap {
      case PrePopResult.PrePopSuccess =>
        result
      case PrePopResult.PrePopFailure(error) =>
        Future.failed(InternalServerException(
          s"[ConfirmedClientResolver] - Failure occurred when pre-populating income source details. Error: $error"
        ))
    }
  }

  private def goToCannotTakePart(implicit request: Request[AnyContent]): Future[Result] = {
    Future.successful(
      Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)
        .addingToSession(ITSASessionKeys.JourneyStateKey -> JourneyStep.SignPosted.key)
        .removingFromSession(FailedClientMatching)
        .clearUserDetailsExceptName
    )
  }

  private def auditIneligibleResult(nino: String, utr: String, arn: String)(implicit request: Request[_]): Future[AuditResult] = {
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(arn),
      utr = Some(utr),
      nino = Some(nino),
      eligibility = "ineligible",
      failureReason = Some("control-list-ineligible")
    ))
  }

  private def auditEligibleResult(nino: String, utr: String, arn: String, eligibilityStatus: EligibilityStatus)
                                 (implicit request: Request[_]): Future[AuditResult] = {
    auditingService.audit(EligibilityAuditModel(
      agentReferenceNumber = Some(arn),
      utr = Some(utr),
      nino = Some(nino),
      eligibility = if (eligibilityStatus.eligibleNextYearOnly) "eligible - next tax year only" else "eligible",
      failureReason = None
    ))
  }

  private def goToSignUpClient(reference: String, eligibilityStatus: EligibilityStatus)
                              (implicit request: IdentifierRequest[AnyContent]): Future[Result] = {
    mandationStatusService.getMandationStatus(request.sessionData) flatMap { mandationStatus =>
      if (isEnabled(WhenDoYouWantToStartPage)) {
        (eligibilityStatus.eligibleCurrentYear, mandationStatus.currentYearStatus, mandationStatus.nextYearStatus) match {
          case (true, Voluntary, Voluntary) =>
            Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show()))
          case (true, Voluntary, Mandated) =>
            Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.NextYearMandatorySignUpController.show()))
          case (true, Mandated, _) =>
            Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.MandatoryBothSignUpController.show))
          case (false, _, Voluntary) =>
            Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.NonEligibleVoluntaryController.show))
          case (false, _, Mandated) =>
            Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.NonEligibleMandatedController.show))
        }
      } else {
        subscriptionDetailsService.fetchEligibilityInterruptPassed(reference) map {
          case Some(_) =>
            if (mandationStatus.currentYearStatus.isMandated || eligibilityStatus.eligibleNextYearOnly) {
              Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
            } else {
              Redirect(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show())
            }
          case None =>
            if (eligibilityStatus.eligibleNextYearOnly) {
              Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
            } else {
              Redirect(controllers.agent.eligibility.routes.ClientCanSignUpController.show())
            }
        }
      }
    }
  }
}