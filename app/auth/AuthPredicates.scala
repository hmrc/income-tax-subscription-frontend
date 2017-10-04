/*
 * Copyright 2017 HM Revenue & Customs
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

package auth

import _root_.uk.gov.hmrc.http.SessionKeys._
import auth.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import auth.JourneyState._
import cats.implicits._
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}

import scala.concurrent.Future

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate = _ => _ => Right(AuthPredicateSuccess)

  lazy val resolveNino: Result = Redirect(controllers.routes.NinoResolverController.resolveNino())

  val ninoPredicate: AuthPredicate = request => user =>
    if (user.nino(request).isDefined) {
      Right(AuthPredicateSuccess)
    }
    else if (user.utr(request).isDefined) {
      Left(Future.failed(new InternalServerException("AuthPredicates.ninoPredicate: unexpected user state, the user has a utr but no nino")))
    } else {
      Left(Future.successful(resolveNino))
    }

  lazy val alreadyEnrolled: Result = Redirect(controllers.routes.AlreadyEnrolledController.enrolled())

  val mtdidPredicate: AuthPredicate = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolled))

  val enrolledPredicate: AuthPredicate = request => user =>
    if (user.mtdItsaRef.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate")))

  lazy val homeRoute = Redirect(controllers.routes.HomeController.index())

  lazy val timeoutRoute = Redirect(controllers.routes.SessionTimeoutController.timeout())

  val timeoutPredicate: AuthPredicate = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  lazy val wrongAffinity: Result = Redirect(controllers.routes.AffinityGroupErrorController.show())

  val affinityPredicate: AuthPredicate = request => user =>
    if (user.affinityGroup contains AffinityGroup.Individual) Right(AuthPredicateSuccess)
    else Left(Future.successful(wrongAffinity))

  val registrationJourneyPredicate: AuthPredicate = request => user =>
    if(request.session.isInState(Registration)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val signUpJourneyPredicate: AuthPredicate = request => user =>
    if(request.session.isInState(Registration) || request.session.isInState(SignUp)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val userMatchingJourneyPredicate: AuthPredicate = request => user =>
    if(request.session.isInState(UserMatching)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  lazy val goToIv = Redirect(controllers.iv.routes.IdentityVerificationController.gotoIV())

  val ivPredicate: AuthPredicate = request => user =>
    if(request.session.isInState(Registration) && user.confidenceLevel < ConfidenceLevel.L200) Left(Future.successful(goToIv))
    else Right(AuthPredicateSuccess)

  val defaultPredicates = timeoutPredicate |+| affinityPredicate |+| ninoPredicate

  val homePredicates = defaultPredicates |+| mtdidPredicate

  val userMatchingPredicates = homePredicates |+| mtdidPredicate |+| userMatchingJourneyPredicate

  val subscriptionPredicates = defaultPredicates |+| mtdidPredicate |+| signUpJourneyPredicate |+| ivPredicate

  val registrationPredicates = defaultPredicates |+| mtdidPredicate |+| registrationJourneyPredicate |+| ivPredicate

  val enrolledPredicates = defaultPredicates |+| enrolledPredicate

}
