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

import common.Constants.ITSASessionKeys
import play.api.mvc._

trait JourneyState {
  val name: String
}

object SignUp extends JourneyState {
  override val name: String = "signUp"
}

object ClaimEnrolment extends JourneyState {
  override val name: String = "claimEnrolment"
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
