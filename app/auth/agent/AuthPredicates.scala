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

package auth.agent

import auth.individual.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import cats.implicits._
import common.Constants
import common.Constants.ITSASessionKeys
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.SessionKeys._

import scala.concurrent.Future

object AuthPredicates extends Results {

  lazy val noArnRoute: Result = Redirect(controllers.agent.matching.routes.NotEnrolledAgentServicesController.show)

  lazy val confirmationRoute: Result = Redirect(controllers.agent.routes.ConfirmationController.show)

  lazy val timeoutRoute: Result = Redirect(controllers.agent.routes.SessionTimeoutController.show)

  val notSubmitted: AuthPredicate[IncomeTaxAgentUser] = request => _ =>
    if (request.session.get(ITSASessionKeys.MTDITID).isEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(confirmationRoute))

  val timeoutPredicate: AuthPredicate[IncomeTaxAgentUser] = request => _ =>
    if (request.session.get(lastRequestTimestamp).nonEmpty && request.session.get(authToken).isEmpty) {
      Left(Future.successful(timeoutRoute))
    }
    else Right(AuthPredicateSuccess)

  val arnPredicate: AuthPredicate[IncomeTaxAgentUser] = _ => user =>
    if (user.enrolments.getEnrolment(Constants.hmrcAsAgent).nonEmpty) Right(AuthPredicateSuccess)
    else Left(Future.successful(noArnRoute))

  val defaultPredicates: AuthPredicate[IncomeTaxAgentUser] = timeoutPredicate |+| arnPredicate

  val homePredicates: AuthPredicate[IncomeTaxAgentUser] = defaultPredicates |+| notSubmitted

}
