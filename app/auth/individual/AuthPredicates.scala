/*
 * Copyright 2020 HM Revenue & Customs
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

import auth.agent.ConfirmAgentSubscription
import auth.individual.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import auth.individual.JourneyState._
import cats.implicits._
import config.AppConfig
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.http.SessionKeys._
import utilities.ITSASessionKeys.JourneyStateKey

import scala.concurrent.Future

trait AuthPredicates extends Results {

  import AuthPredicates._

  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val alreadyEnrolled: Result = Redirect(controllers.individual.subscription.routes.AlreadyEnrolledController.show())

  val mtdidPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolled))

  val enrolledPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate")))

  lazy val homeRoute: Result = Redirect(controllers.usermatching.routes.HomeController.index())

  lazy val cannotUseServiceRoute: Result = Redirect(controllers.individual.incomesource.routes.CannotUseServiceController.show())

  lazy val timeoutRoute: Result  = Redirect(controllers.routes.SessionTimeoutController.show())

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  lazy val wrongAffinity: Result = Redirect(controllers.usermatching.routes.AffinityGroupErrorController.show())

  val affinityPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    user.affinityGroup match {
      case Some(Individual) | Some(Organisation) => Right(AuthPredicateSuccess)
      case _ => Left(Future.successful(wrongAffinity))
    }

  val registrationJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val signUpJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration) || request.session.isInState(SignUp))
      Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val preferencesJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration) || request.session.isInState(SignUp)
      || request.session.isInState(ConfirmAgentSubscription))
      Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val userMatchingJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(UserMatching)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val administratorRolePredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (!user.isAssistant) Right(AuthPredicateSuccess)
    else Left(Future.successful(cannotUseServiceRoute))

  lazy val goToIv: Result = Redirect(controllers.individual.routes.IdentityVerificationController.gotoIV())

  val ivPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration) && user.confidenceLevel < ConfidenceLevel.L200) Left(Future.successful(goToIv))
    else Right(AuthPredicateSuccess)

  val confirmedAgentPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.hasConfirmedAgent) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val defaultPredicates: AuthPredicate[IncomeTaxSAUser] = timeoutPredicate |+| affinityPredicate |+| ninoPredicate

  val homePredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+| timeoutPredicate |+| affinityPredicate |+| mtdidPredicate

  val userMatchingPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    timeoutPredicate |+| affinityPredicate |+| mtdidPredicate |+| userMatchingJourneyPredicate

  val subscriptionPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    defaultPredicates |+| mtdidPredicate |+| signUpJourneyPredicate |+| ivPredicate

  val registrationPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    defaultPredicates |+| mtdidPredicate |+| registrationJourneyPredicate |+| ivPredicate

  val enrolledPredicates: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+| timeoutPredicate |+| enrolledPredicate

  val preferencesPredicate: AuthPredicate[IncomeTaxSAUser] = administratorRolePredicate |+|
    defaultPredicates |+| mtdidPredicate |+| preferencesJourneyPredicate |+| ivPredicate

}

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val userMatching: Result = Redirect(controllers.usermatching.routes.UserDetailsController.show())

  val ninoPredicate: AuthPredicate[IncomeTaxSAUser] = implicit request => user =>
    if (user.nino.isDefined) Right(AuthPredicateSuccess)
    else if (user.utr.isDefined) Left(Future.successful(homeRoute))
    else Left(Future.successful(userMatching withJourneyState UserMatching))

  lazy val alreadyEnrolledRoute: Result = Redirect(controllers.individual.subscription.routes.AlreadyEnrolledController.show())

  lazy val notEnrolledPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolledRoute))

  lazy val homeRoute: Result = Redirect(controllers.usermatching.routes.HomeController.index())

  lazy val timeoutRoute: Result = Redirect(controllers.routes.SessionTimeoutController.show())

  lazy val wrongAffinity: Result = Redirect(controllers.usermatching.routes.AffinityGroupErrorController.show())

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  def affinityPredicate(implicit appConfig: AppConfig): AuthPredicate[IncomeTaxSAUser] = request => user =>
    user.affinityGroup match {
      case Some(Individual) | Some(Organisation) => Right(AuthPredicateSuccess)
      case _ => Left(Future.successful(wrongAffinity))
    }

  def defaultPredicates(implicit appConfig: AppConfig): AuthPredicate[IncomeTaxSAUser] =
    timeoutPredicate |+| affinityPredicate |+| ninoPredicate

  val journeyStatePredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if ((request.session get JourneyStateKey).isDefined) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

}
