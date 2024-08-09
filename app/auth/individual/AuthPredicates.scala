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

package auth.individual

import auth.individual.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import auth.individual.JourneyState._
import cats.implicits._
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.JourneyStateKey
import config.AppConfig
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.IVHandoffAuditing.IVHandoffAuditModel
import play.api.Logging
import play.api.mvc.{Result, Results}
import services.AuditingService
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.Future

trait AuthPredicates extends Results with FrontendHeaderCarrierProvider with Logging {

  val appConfig: AppConfig
  val auditingService: AuditingService

  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val alreadyEnrolled: Result = Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show)

  val mtdidPredicate: AuthPredicate[IncomeTaxSAUser] = implicit request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else {
      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = None,
        utr = None, // todo: same as below
        nino = None, //todo: can't get nino here yet, will add when we establish better auth
        eligibility = "ineligible",
        failureReason = Some("already-signed-up-with-mtd-id")
      ))
      Left(Future.successful(alreadyEnrolled))
    }

  val enrolledPredicate: AuthPredicate[IncomeTaxSAUser] = _ => user =>
    if (user.mtdItsaRef.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate")))

  lazy val homeRoute: Result = Redirect(controllers.individual.matching.routes.HomeController.index)

  lazy val claimEnrolmentRoute: Result = Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show)

  lazy val cannotUseServiceRoute: Result = Redirect(controllers.individual.matching.routes.CannotUseServiceController.show())

  lazy val timeoutRoute: Result = Redirect(controllers.individual.routes.SessionTimeoutController.show)

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  lazy val wrongAffinity: Result = Redirect(controllers.individual.matching.routes.AffinityGroupErrorController.show)

  val affinityPredicate: AuthPredicate[IncomeTaxSAUser] = _ => user =>
    user.affinityGroup match {
      case Some(Individual) | Some(Organisation) => Right(AuthPredicateSuccess)
      case _ =>
        Left(Future.successful(wrongAffinity))
    }

  val signUpJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.isInState(SignUp))
      Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val spsEntityIdPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.get(ITSASessionKeys.SPSEntityId).isDefined)
      Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val claimEnrolmentJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.isInState(ClaimEnrolment))
      Right(AuthPredicateSuccess)
    else Left(Future.successful(claimEnrolmentRoute))

  val preferencesJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.isInState(SignUp)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val administratorRolePredicate: AuthPredicate[IncomeTaxSAUser] = implicit request => user =>
    if (!user.isAssistant) Right(AuthPredicateSuccess)
    else {
      auditingService.audit(EligibilityAuditModel(
        agentReferenceNumber = None,
        utr = None, //todo: same as below
        nino = None, //todo: can't get nino here yet, will add when we establish better auth
        eligibility = "ineligible",
        failureReason = Some("user-type-assistant")
      ))
      Left(Future.successful(cannotUseServiceRoute))
    }


  val ivPredicate: AuthPredicate[IncomeTaxSAUser] = implicit request => user => {
    if (user.confidenceLevel >= appConfig.identityVerificationRequiredConfidenceLevel) {
      Right(AuthPredicateSuccess)
    } else {
      user.affinityGroup match {
        case Some(Individual) =>
          logger.info("[AuthPredicates][ivPredicate] - Redirecting individual to IV")
          auditingService.audit(IVHandoffAuditModel(
            handoffReason = "individual",
            currentConfidence = user.confidenceLevel.level,
            minConfidence = appConfig.identityVerificationRequiredConfidenceLevel.level
          ))
          Left(Future.successful(Redirect(appConfig.identityVerificationURL)
            .addingToSession(ITSASessionKeys.IdentityVerificationFlag -> "true")))
        case Some(Organisation) =>
          logger.info("[AuthPredicates][ivPredicate] - Redirecting organisation to IV")
          auditingService.audit(IVHandoffAuditModel(
            handoffReason = "organisation",
            currentConfidence = user.confidenceLevel.level,
            minConfidence = appConfig.identityVerificationRequiredConfidenceLevel.level
          ))
          Left(Future.successful(Redirect(appConfig.identityVerificationURL)
            .addingToSession(ITSASessionKeys.IdentityVerificationFlag -> "true")))
        case _ => Right(AuthPredicateSuccess)
      }
    }
  }

  val defaultPredicates: AuthPredicate[IncomeTaxSAUser] = timeoutPredicate |+| affinityPredicate |+| ivPredicate

  val homePredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+| timeoutPredicate |+| affinityPredicate |+| mtdidPredicate |+| ivPredicate

  val subscriptionPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    defaultPredicates |+| mtdidPredicate |+| signUpJourneyPredicate |+| spsEntityIdPredicate

  val claimEnrolmentPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+| affinityPredicate |+|
    ivPredicate |+| claimEnrolmentJourneyPredicate

  val enrolledPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+| timeoutPredicate |+| enrolledPredicate |+| ivPredicate

  val preferencesPredicate: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    defaultPredicates |+| mtdidPredicate |+| preferencesJourneyPredicate

}

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val alreadyEnrolledRoute: Result = Redirect(controllers.individual.matching.routes.AlreadyEnrolledController.show)

  lazy val notEnrolledPredicate: AuthPredicate[IncomeTaxSAUser] = _ => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolledRoute))

  lazy val homeRoute: Result = Redirect(controllers.individual.matching.routes.HomeController.index)

  lazy val timeoutRoute: Result = Redirect(controllers.individual.routes.SessionTimeoutController.show)

  lazy val wrongAffinity: Result = Redirect(controllers.individual.matching.routes.AffinityGroupErrorController.show)

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  def affinityPredicate: AuthPredicate[IncomeTaxSAUser] = _ => user =>
    user.affinityGroup match {
      case Some(Individual) | Some(Organisation) => Right(AuthPredicateSuccess)
      case _ => Left(Future.successful(wrongAffinity))
    }

  val journeyStatePredicate: AuthPredicate[IncomeTaxSAUser] = request => _ =>
    if ((request.session get JourneyStateKey).isDefined) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

}
