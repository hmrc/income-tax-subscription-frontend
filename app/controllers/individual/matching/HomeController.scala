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

package controllers.individual.matching

import auth.individual.JourneyState.*
import auth.individual.SignUp
import common.Constants.ITSASessionKeys
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, PreSignUpJourneyRefiner}
import controllers.individual.resolvers.AlreadySignedUpResolver
import controllers.utils.ReferenceRetrieval
import models.{Channel, EligibilityStatus, SessionData}
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.SignupStartedAuditing
import models.common.subscription.SubscriptionSuccess
import models.requests.individual.PreSignUpRequest
import play.api.mvc.*
import services.PrePopDataService.PrePopResult
import services.*
import services.individual.*
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(identity: IdentifierAction,
                               journeyRefiner: PreSignUpJourneyRefiner,
                               sessionDataService: SessionDataService,
                               citizenDetailsService: CitizenDetailsService,
                               throttlingService: ThrottlingService,
                               subscriptionService: SubscriptionService,
                               auditingService: AuditingService,
                               eligibilityStatusService: GetEligibilityStatusService,
                               referenceRetrieval: ReferenceRetrieval,
                               resolver: AlreadySignedUpResolver,
                               prePopDataService: PrePopDataService)
                              (implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController {

  def index: Action[AnyContent] = (identity andThen journeyRefiner) async { implicit request =>
    withMaybeUtrAndName { case (maybeUtr, maybeName) =>
      startIndividualSignupAudit(request.nino, maybeUtr) flatMap { _ =>
        maybeUtr match {
          case Some(utr) =>
            val nameCookie = maybeName.map(ITSASessionKeys.FULLNAME -> _)
            for {
              _ <- sessionDataService.saveUTR(utr)
              result <- handlePresentUTR(utr)
            } yield {
              result.addingToSession(nameCookie.toSeq: _*)
            }
          case None => handleNoUTR
        }
      }
    }
  }

  private def handlePresentUTR(utr: String)(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    sessionDataService.getAllSessionData().flatMap { sessionData =>
      throttlingService.throttled(IndividualStartOfJourneyThrottle, sessionData) {
        subscriptionService.getSubscription(request.nino) flatMap {
          case Right(Some(SubscriptionSuccess(_, channel))) => resolver.resolve(sessionData, request.isEnrolled, channel)
          case Right(None) => handleNoSubscriptionFound(sessionData, utr)
          case Left(error) => throw new InternalServerException(s"[HomeController][handlePresentUTR] - Error fetching subscription: $error")
        }
      }
    }
  }

  private def handleNoSubscriptionFound(sessionData: SessionData, utr: String)(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    eligibilityStatusService.getEligibilityStatus(sessionData) flatMap {
      case EligibilityStatus(false, false, _) =>
        handleIneligible(utr)
      case EligibilityStatus(eligibleCurrentYear, _, _) =>
        handleEligible(sessionData, utr, eligibleCurrentYear)
    }
  }

  private def handleEligible(sessionData: SessionData, utr: String, eligibleCurrentYear: Boolean)(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    eligibilityAudit(
      maybeUTR = Some(utr),
      eligibility = if (eligibleCurrentYear) "eligible" else "eligible - next year only"
    ) flatMap { _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference(sessionData)
        prePopResult <- prePopDataService.prePopIncomeSources(reference, request.nino)
      } yield {
        prePopResult match {
          case PrePopResult.PrePopSuccess => goToSignUp(eligibleCurrentYear).withJourneyState(SignUp)
          case PrePopResult.PrePopFailure(error) =>
            throw new InternalServerException(
              s"[HomeController][handleNoSubscriptionFound] - Failure occurred when pre-populating income source details. Error: $error"
            )
        }
      }
    }
  }

  private def goToSignUp(eligibleCurrentYear: Boolean): Result = {
    if (eligibleCurrentYear) {
      Redirect(controllers.individual.routes.YouCanSignUpController.show)
    } else {
      Redirect(controllers.individual.controllist.routes.CannotSignUpThisYearController.show)
    }
  }

  private def withMaybeUtrAndName(f: (Option[String], Option[String]) => Future[Result])(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    request.utr match {
      case Some(utr) =>
        f(Some(utr), None)
      case None =>
        citizenDetailsService.lookupCitizenDetails(request.nino) flatMap { details =>
          f(details.utr, details.name)
        }
    }
  }

  private def handleNoUTR(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    eligibilityAudit(
      maybeUTR = None,
      eligibility = "ineligible",
      failureReason = Some("no-self-assessment")
    ) map { _ =>
      Redirect(routes.NoSAController.show)
    }
  }

  private def handleIneligible(utr: String)(implicit request: PreSignUpRequest[AnyContent]): Future[Result] = {
    eligibilityAudit(
      maybeUTR = Some(utr),
      eligibility = "ineligible",
      failureReason = Some("control-list-ineligible")
    ) map { _ =>
      Redirect(controllers.individual.controllist.routes.NotEligibleForIncomeTaxController.show())
    }
  }

  private def eligibilityAudit(maybeUTR: Option[String], eligibility: String, failureReason: Option[String] = None)
                              (implicit request: PreSignUpRequest[AnyContent]): Future[AuditResult] = {
    val auditModel = EligibilityAuditModel(
      agentReferenceNumber = None,
      utr = maybeUTR,
      nino = Some(request.nino),
      eligibility = eligibility,
      failureReason = failureReason
    )
    auditingService.audit(auditModel)
  }

  private def startIndividualSignupAudit(nino: String, utr: Option[String])(implicit request: Request[_]): Future[AuditResult] = {
    val auditModel = SignupStartedAuditing.SignupStartedAuditModel(
      agentReferenceNumber = None,
      utr = utr,
      nino = Some(nino)
    )
    auditingService.audit(auditModel)
  }

}