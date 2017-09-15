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
import _root_.uk.gov.hmrc.http.SessionKeys._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ InternalServerException, NotFoundException }

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate = _ => _ => Right(AuthPredicateSuccess)

  lazy val noNino: Result = Redirect(controllers.routes.NoNinoController.showNoNino())

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

  val registrationPredicates = defaultPredicates |+| mtdidPredicate |+| goHomePredicate

  val enrolledPredicates = defaultPredicates |+| enrolledPredicate

}
