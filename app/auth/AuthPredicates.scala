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

import auth.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import cats.implicits._
import common.Constants
import controllers.ITSASessionKeys
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.http.SessionKeys._

import scala.concurrent.Future

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate = _ => _ => Right(AuthPredicateSuccess)

  lazy val noNino: Result = Redirect(controllers.routes.NoNinoController.showNoNino())

  lazy val iv: Result = Redirect(controllers.iv.routes.IdentityVerificationController.gotoIV())

  val ninoPredicate: AuthPredicate = request => user =>
    if (user.enrolments.getEnrolment(Constants.ninoEnrolmentName).isDefined) {
      Right(AuthPredicateSuccess)
    }
    else Left(Future.successful(iv))

  lazy val alreadyEnrolled: Result = Redirect(controllers.routes.AlreadyEnrolledController.enrolled())

  val mtdidPredicate: AuthPredicate = request => user =>
    if (user.mtdItsaRef.isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(alreadyEnrolled))

  val enrolledPredicate: AuthPredicate = request => user =>
    if (user.mtdItsaRef.nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("AuthPredicates.enrolledPredicate")))

  lazy val homeRoute = Redirect(controllers.routes.HomeController.index())

  val goHomePredicate: AuthPredicate = request => user =>
    if (request.session.get(ITSASessionKeys.GoHome).nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

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

  val defaultPredicates = timeoutPredicate |+| affinityPredicate |+| ninoPredicate

  val homePredicates = defaultPredicates |+| mtdidPredicate

  val subscriptionPredicates = defaultPredicates |+| mtdidPredicate |+| goHomePredicate

  val enrolledPredicates = defaultPredicates |+| enrolledPredicate

}
