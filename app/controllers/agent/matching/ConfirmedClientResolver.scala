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

import auth.agent.{AgentSignUp, UserMatchingController}
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.{FailedClientMatching, JourneyStateKey, NINO, UTR}
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import connectors.MandationStatusConnector
import controllers.utils.ReferenceRetrieval
import models.status.MandationStatus.Mandated
import models.status.MandationStatusModel
import models.{EligibilityStatus, PrePopData}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmedClientResolver @Inject()(getEligibilityStatusService: GetEligibilityStatusService,
                                        mandationStatusConnector: MandationStatusConnector,
                                        throttlingService: ThrottlingService,
                                        prePopulationService: PrePopulationService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val sessionDataService: SessionDataService,
                                        val appConfig: AppConfig,
                                        val subscriptionDetailsService: SubscriptionDetailsService)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends UserMatchingController with ReferenceRetrieval with FeatureSwitching {

  def resolve: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val utr: String = user.clientUtr.getOrElse(throw new InternalServerException("[ConfirmedClientResolver][resolve] - utr not present"))
      val nino: String = user.clientNino.getOrElse(throw new InternalServerException("[ConfirmedClientResolver][resolve] - nino not present"))
      val arn: String = user.arn

      throttlingService.throttled(AgentStartOfJourneyThrottle) {
        withEligibilityResult(utr) {
          case EligibilityStatus(false, false, _) =>
            Future.successful(
              goToCannotTakePart
                .removingFromSession(FailedClientMatching, JourneyStateKey, UTR, NINO)
                .clearAllUserDetails
            )
          case EligibilityStatus(thisYear, _, prepop) =>
            withReference(utr, Some(nino), Some(arn)) { reference =>
              handlePrepop(reference, prepop) flatMap { _ =>
                withMandationStatus(nino, utr) { mandationStatus =>
                  goToSignUpClient(nextYearOnly = !thisYear)
                    .addingToSession(ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> (!thisYear).toString)
                    .addingToSession(ITSASessionKeys.MANDATED_CURRENT_YEAR -> (mandationStatus.currentYearStatus == Mandated).toString)
                    .addingToSession(ITSASessionKeys.MANDATED_NEXT_YEAR -> (mandationStatus.nextYearStatus == Mandated).toString)
                    .addingToSession(JourneyStateKey -> AgentSignUp.name)
                }
              }
            }

        }
      }
  }

  private def withEligibilityResult(utr: String)(f: EligibilityStatus => Future[Result])
                                   (implicit request: Request[AnyContent]): Future[Result] = {
    getEligibilityStatusService.getEligibilityStatus(utr) flatMap {
      case Left(value) =>
        throw new InternalServerException(
          s"[ConfirmClientController][withEligibilityResult] - call to control list failed with status: ${value.httpResponse.status}"
        )
      case Right(result) =>
        f(result)
    }
  }

  private def withMandationStatus(nino: String, utr: String)
                                 (f: MandationStatusModel => Result)
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    mandationStatusConnector.getMandationStatus(nino, utr) map {
      case Left(_) =>
        throw new InternalServerException("[ConfirmClientController][withMandationStatus] - Unexpected failure when receiving mandation status")
      case Right(model) =>
        f(model)
    }
  }

  private def goToSignUpClient(nextYearOnly: Boolean): Result = {
    if (nextYearOnly) {
      Redirect(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show)
    } else {
      Redirect(controllers.agent.eligibility.routes.ClientCanSignUpController.show())
    }
  }

  private def goToCannotTakePart: Result =
    Redirect(controllers.agent.eligibility.routes.CannotTakePartController.show)

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

}
