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

import agent.controllers.ITSASessionKeys
import play.api.mvc._

sealed trait JourneyState {
  val name: String
}

object UserMatching extends JourneyState {
  override val name: String = "userMatching"
}

object UserMatched extends JourneyState {
  override val name: String = "userMatched"
}

object SignUp extends JourneyState {
  override val name: String = "signUp"
}

object Registration extends JourneyState {
  override val name: String = "registration"
}

object JourneyState {

  implicit class SessionFunctions(session: Session) {
    def isInState(state: JourneyState): Boolean = session.get(ITSASessionKeys.JourneyStateKey) contains state.name
  }

  implicit class RequestFunctions(request: Request[_]) {
    def isInState(state: JourneyState): Boolean = request.session.isInState(state)
  }

  implicit class ResultFunctions(result: Result) {
    def withJourneyState(state: JourneyState)(implicit header: RequestHeader): Result = result.addingToSession(ITSASessionKeys.JourneyStateKey -> state.name)
  }

}
