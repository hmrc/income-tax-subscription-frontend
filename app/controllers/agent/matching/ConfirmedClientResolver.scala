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
import models.agent.JourneyStep
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.{EligibilityStatus, SessionData}
import play.api.mvc.*
import services.*
import services.PrePopDataService.PrePopResult
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
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
    val sessionData = request.sessionData
    throttlingService.throttled(AgentStartOfJourneyThrottle, sessionData) {
      getEligibilityStatusService.getEligibilityStatus(sessionData) flatMap {
        case EligibilityStatus(false, false, _) =>
          for {
            nino <- ninoService.getNino(sessionData)
            utr <- utrService.getUTR(sessionData)
            _ <- auditingService.audit(EligibilityAuditModel(
              agentReferenceNumber = Some(request.arn),
              utr = Some(utr),
              nino = Some(nino),
              eligibility = "ineligible",
              failureReason = Some("control-list-ineligible")
            ))
          } yield {
            Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)
              .addingToSession(ITSASessionKeys.JourneyStateKey -> JourneyStep.SignPosted.key)
              .removingFromSession(FailedClientMatching)
              .clearUserDetailsExceptName
          }
        case EligibilityStatus(thisYear, _, _) =>
          for {
            result <- goToSignUpClient(
              arn = request.arn,
              nextYearOnly = !thisYear,
              sessionData = sessionData
            )
          } yield {
            result.addingToSession(
              JourneyStateKey -> AgentSignUp.name
            )
          }
      }
    }
  }

  private def goToSignUpClient(arn: String, nextYearOnly: Boolean, sessionData: SessionData)
                              (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    for {
      reference <- referenceRetrieval.getReference(Some(arn), sessionData)
      nino <- ninoService.getNino(sessionData)
      utr <- utrService.getUTR(sessionData)
      _ <- auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = Some(arn),
        utr = Some(utr),
        nino = Some(nino),
        eligibility = if (nextYearOnly) "eligible - next tax year only" else "eligible",
        failureReason = None
      ))
      eligibilityInterrupt <- subscriptionDetailsService.fetchEligibilityInterruptPassed(reference)
      mandationStatus <- mandationStatusService.getMandationStatus(sessionData)
      prePopResult <- prePopDataService.prePopIncomeSources(reference, nino)
    } yield {
      prePopResult match {
        case PrePopResult.PrePopSuccess =>
          val isVoluntaryCurrentYear: Boolean = mandationStatus.currentYearStatus.isVoluntary
          val isVoluntaryNextYear: Boolean = mandationStatus.nextYearStatus.isVoluntary
          val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
          val isMandatedNextYear: Boolean = mandationStatus.nextYearStatus.isMandated

          if (isEnabled(WhenDoYouWantToStartPage) && isVoluntaryCurrentYear && isVoluntaryNextYear) {
            Redirect(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show())
          } else if (isEnabled(WhenDoYouWantToStartPage) && isMandatedCurrentYear && isMandatedNextYear) {
              Redirect(controllers.agent.tasklist.taxyear.routes.MandatoryBothSignUpController.show())
        } else {
            eligibilityInterrupt match {
              case Some(_) =>
                val isEligibleNextYearOnly: Boolean = nextYearOnly

                if (isMandatedCurrentYear || isEligibleNextYearOnly) {
                  Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
                } else {
                  Redirect(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show())
                }
              case None =>
                if (nextYearOnly) {
                  Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
                } else {
                  Redirect(controllers.agent.eligibility.routes.ClientCanSignUpController.show())
                }
            }
          }
        case PrePopResult.PrePopFailure(error) =>
          throw new InternalServerException(
            s"[ConfirmedClientResolver] - Failure occurred when pre-populating income source details. Error: $error"
          )
      }
    }
  }
}