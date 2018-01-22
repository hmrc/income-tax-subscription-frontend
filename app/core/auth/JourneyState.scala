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

import core.ITSASessionKeys
import core.auth.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import play.api.mvc._
import AuthPredicates._
import JourneyState._

import scala.concurrent.Future

trait JourneyState {
  val name: String

  lazy val journeyStatePredicate: AuthPredicate[IncomeTaxSAUser] = request => user =>
    if (request.isInState(this)) Right(AuthPredicateSuccess)
    else Left(Future.successful(homeRoute))
}

object UserMatching extends JourneyState {
  override val name: String = "userMatching"
}

object SignUp extends JourneyState {
  override val name: String = "signUp"
}

object Registration extends JourneyState {
  override val name: String = "registration"
}

object UserMatched extends JourneyState {
  override val name: String = "userMatched"
}

object JourneyState {

  implicit class SessionFunctions(session: Session) {
    def isInState(state: JourneyState): Boolean = session.get(ITSASessionKeys.JourneyStateKey) contains state.name
    def hasConfirmedAgent: Boolean = session.get(ITSASessionKeys.ConfirmedAgent).fold(false)(_.toBoolean)
  }

  implicit class RequestFunctions(request: Request[_]) {
    def isInState(state: JourneyState): Boolean = request.session.isInState(state)
    def hasConfirmedAgent: Boolean = request.session.hasConfirmedAgent
  }

  implicit class ResultFunctions(result: Result) {
    def withJourneyState(state: JourneyState)(implicit header: RequestHeader): Result = result.addingToSession(ITSASessionKeys.JourneyStateKey -> state.name)
    def confirmAgent(implicit header: RequestHeader): Result =
      result.addingToSession(ITSASessionKeys.ConfirmedAgent -> true.toString)
  }

}
