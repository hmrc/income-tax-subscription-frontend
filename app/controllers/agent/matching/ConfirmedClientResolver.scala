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

import auth.agent.{AgentSignUp, IncomeTaxAgentUser, UserMatchingController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{FailedClientMatching, JourneyStateKey}
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.EligibilityStatus
import models.agent.JourneyStep
import models.audits.EligibilityAuditing.EligibilityAuditModel
import play.api.mvc._
import services.PrePopDataService.PrePopResult
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedClientResolver @Inject()(getEligibilityStatusService: GetEligibilityStatusService,
                                        throttlingService: ThrottlingService,
                                        referenceRetrieval: ReferenceRetrieval,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        prePopDataService: PrePopDataService,
                                        ninoService: NinoService,
                                        utrService: UTRService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends UserMatchingController {

  def resolve: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val arn: String = user.arn

      throttlingService.throttled(AgentStartOfJourneyThrottle) {
        getEligibilityStatusService.getEligibilityStatus flatMap {
          case EligibilityStatus(false, false) =>
            for {
              nino <- ninoService.getNino
              utr <- utrService.getUTR
              _ <- auditingService.audit(EligibilityAuditModel(
                agentReferenceNumber = Some(arn),
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
          case EligibilityStatus(thisYear, _) =>
            for {
              result <- goToSignUpClient(nextYearOnly = !thisYear)
            } yield {
              result.addingToSession(
                JourneyStateKey -> AgentSignUp.name
              )
            }
        }
      }
  }

  private def goToSignUpClient(nextYearOnly: Boolean)
                              (implicit hc: HeaderCarrier, request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    for {
      reference <- referenceRetrieval.getAgentReference
      nino <- ninoService.getNino
      utr <- utrService.getUTR
      _ <- auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = Some(user.arn),
        utr = Some(utr),
        nino = Some(nino),
        eligibility = if (nextYearOnly) "eligible - next tax year only" else "eligible",
        failureReason = None
      ))
      eligibilityInterrupt <- subscriptionDetailsService.fetchEligibilityInterruptPassed(reference)
      prePopResult <- prePopDataService.prePopIncomeSources(reference)
    } yield {
      prePopResult match {
        case PrePopResult.PrePopSuccess =>
          eligibilityInterrupt match {
            case Some(_) =>
              Redirect(controllers.agent.routes.WhatYouNeedToDoController.show())
            case None =>
              if (nextYearOnly) {
                Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
              } else {
                Redirect(controllers.agent.eligibility.routes.ClientCanSignUpController.show())
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
