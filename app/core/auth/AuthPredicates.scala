/*
 * Copyright 2018 HM Revenue & Customs
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

package core.auth

import _root_.uk.gov.hmrc.http.SessionKeys._
import cats.implicits._
import core.auth.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import core.auth.JourneyState._
import core.config.AppConfig
import play.api.mvc.{Call, Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}

import scala.concurrent.Future

trait AuthPredicates extends Results {
  def applicationConfig: AppConfig

  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val resolveNino: Result = Redirect(usermatching.controllers.routes.NinoResolverController.resolveNinoAction())

  val ninoPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.nino(request).isDefined) {
      Right(AuthPredicateSuccess)
    }
    else if (user.utr(request).isDefined) {
      Left(Future.failed(new InternalServerException("AuthPredicates.ninoPredicate: unexpected user state, the user has a utr but no nino")))
    } else {
      Left(Future.successful(resolveNino))
    }

  lazy val alreadyEnrolled: Result = Redirect(incometax.subscription.controllers.routes.AlreadyEnrolledController.show())

  val mtdidPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolled))

  val enrolledPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate")))

  lazy val homeRoute = Redirect(usermatching.controllers.routes.HomeController.index())

  lazy val timeoutRoute = Redirect(core.controllers.routes.SessionTimeoutController.show())

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  lazy val wrongAffinity: Result = Redirect(usermatching.controllers.routes.AffinityGroupErrorController.show())

  val affinityPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    (applicationConfig.userMatchingFeature, user.affinityGroup) match {
      case (true, Some(Individual) | Some(Organisation)) => Right(AuthPredicateSuccess)
      case (false, Some(Individual)) => Right(AuthPredicateSuccess)
      case _ => Left(Future.successful(wrongAffinity))
    }

  val registrationJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val signUpJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration) || request.session.isInState(SignUp)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val userMatchingJourneyPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(UserMatching)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  lazy val goToIv = Redirect(identityverification.controllers.routes.IdentityVerificationController.gotoIV())

  val ivPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.isInState(Registration) && user.confidenceLevel < ConfidenceLevel.L200) Left(Future.successful(goToIv))
    else Right(AuthPredicateSuccess)

  val newIncomeSourceFlowFeature: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if(applicationConfig.newIncomeSourceFlowEnabled) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.newIncomeSourceFlowFeature")))

  val oldIncomeSourceFlowFeature: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if(!applicationConfig.newIncomeSourceFlowEnabled) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.oldIncomeSourceFlowFeature")))

  val taxYearDeferralFeature: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if(applicationConfig.taxYearDeferralEnabled) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.taxYearDeferralFeature")))


  val defaultPredicates = timeoutPredicate |+| affinityPredicate |+| ninoPredicate

  val homePredicates = defaultPredicates |+| mtdidPredicate

  val userMatchingPredicates = timeoutPredicate |+| affinityPredicate |+| mtdidPredicate |+| userMatchingJourneyPredicate

  val subscriptionPredicates = defaultPredicates |+| mtdidPredicate |+| signUpJourneyPredicate |+| ivPredicate

  val registrationPredicates = defaultPredicates |+| mtdidPredicate |+| registrationJourneyPredicate |+| ivPredicate

  val enrolledPredicates = timeoutPredicate |+| enrolledPredicate

}

object AuthPredicates extends Results {
  val emptyPredicate: AuthPredicate[IncomeTaxSAUser] = _ => _ => Right(AuthPredicateSuccess)

  lazy val resolveNino: Result = Redirect(usermatching.controllers.routes.NinoResolverController.resolveNinoAction())

  val ninoPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.nino(request).isDefined) {
      Right(AuthPredicateSuccess)
    }
    else if (user.utr(request).isDefined) {
      Left(Future.failed(new InternalServerException("AuthPredicates.ninoPredicate: unexpected user state, the user has a utr but no nino")))
    } else {
      Left(Future.successful(resolveNino))
    }

  lazy val alreadyEnrolledRoute: Result = Redirect(incometax.subscription.controllers.routes.AlreadyEnrolledController.show())

  lazy val notEnrolledPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolledRoute))

  lazy val homeRoute = Redirect(usermatching.controllers.routes.HomeController.index())

  lazy val timeoutRoute = Redirect(core.controllers.routes.SessionTimeoutController.show())

  lazy val wrongAffinity: Result = Redirect(usermatching.controllers.routes.AffinityGroupErrorController.show())

  val timeoutPredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  def affinityPredicate(implicit appConfig: AppConfig): AuthPredicate[IncomeTaxSAUser] = request => user =>
    (appConfig.userMatchingFeature, user.affinityGroup) match {
      case (true, Some(Individual) | Some(Organisation)) => Right(AuthPredicateSuccess)
      case (false, Some(Individual)) => Right(AuthPredicateSuccess)
      case _ => Left(Future.successful(wrongAffinity))
    }

  def defaultPredicates(implicit appConfig: AppConfig): AuthPredicate[IncomeTaxSAUser] =
    timeoutPredicate |+| affinityPredicate |+| ninoPredicate
}
