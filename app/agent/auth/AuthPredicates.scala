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

package agent.auth

import _root_.uk.gov.hmrc.http.SessionKeys._
import agent.auth.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import agent.auth.JourneyState._
import cats.implicits._
import agent.common.Constants.agentServiceEnrolmentName
import agent.controllers.ITSASessionKeys
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

object AuthPredicates extends Results {

  val emptyPredicate: AuthPredicate = _ => _ => Right(AuthPredicateSuccess)

  lazy val noArnRoute: Result = Redirect(agent.controllers.routes.NotEnrolledAgentServicesController.show())

  lazy val confirmationRoute: Result = Redirect(agent.controllers.routes.ConfirmationController.showConfirmation())

  lazy val timeoutRoute = Redirect(agent.controllers.routes.SessionTimeoutController.timeout())

  lazy val homeRoute = Redirect(agent.controllers.routes.HomeController.index())


  val notSubmitted: AuthPredicate = request => user =>
    if (request.session.get(ITSASessionKeys.Submitted).isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(confirmationRoute))

  val hasSubmitted: AuthPredicate = request => user =>
    if (request.session.get(ITSASessionKeys.Submitted).nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.failed(new NotFoundException("auth.AuthPredicates.hasSubmitted")))

  val timeoutPredicate: AuthPredicate = request => user =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  val registrationJourneyPredicate: AuthPredicate = request => user =>
    if (request.session.isInState(Registration)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val signUpJourneyPredicate: AuthPredicate = request => user =>
    if (request.session.isInState(Registration) || request.session.isInState(SignUp)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val userMatchingJourneyPredicate: AuthPredicate = request => user =>
    if (request.session.isInState(UserMatching)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))

  val arnPredicate: AuthPredicate = request => user =>
    if (user.enrolments.getEnrolment(agentServiceEnrolmentName).nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(noArnRoute))


  val defaultPredicates = timeoutPredicate |+| arnPredicate

  val homePredicates = defaultPredicates |+| notSubmitted

  val userMatchingPredicates = homePredicates |+| userMatchingJourneyPredicate

  val subscriptionPredicates = homePredicates |+| signUpJourneyPredicate

  val registrationPredicates = homePredicates |+| registrationJourneyPredicate

  val confirmationPredicates = defaultPredicates |+| hasSubmitted

}
