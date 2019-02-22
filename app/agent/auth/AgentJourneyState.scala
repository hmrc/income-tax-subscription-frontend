/*
 * Copyright 2019 HM Revenue & Customs
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

sealed trait AgentJourneyState {
  val name: String
}

object AgentUserMatching extends AgentJourneyState {
  override val name: String = "userMatching"
}

object AgentUserMatched extends AgentJourneyState {
  override val name: String = "userMatched"
}

object AgentSignUp extends AgentJourneyState {
  override val name: String = "signUp"
}

object AgentRegistration extends AgentJourneyState {
  override val name: String = "registration"
}

object AgentJourneyState {

  implicit class SessionFunctions(session: Session) {
    def isInState(state: AgentJourneyState): Boolean = session.get(ITSASessionKeys.JourneyStateKey) contains state.name
    def isUnauthorisedAgent: Boolean = session.get(ITSASessionKeys.UnauthorisedAgentKey).fold(false)(_.toBoolean)
  }

  implicit class RequestFunctions(request: Request[_]) {
    def isInAgentState(state: AgentJourneyState): Boolean = request.session.isInState(state)
    def isUnauthorisedAgent: Boolean = request.session.isUnauthorisedAgent
  }

  implicit class ResultFunctions(result: Result) {
    def withJourneyState(state: AgentJourneyState)(implicit header: RequestHeader): Result =
      result.addingToSession(ITSASessionKeys.JourneyStateKey -> state.name)
    def setUnauthorisedAgent(isUnauthorised: Boolean)(implicit header: RequestHeader): Result =
      result.addingToSession(ITSASessionKeys.UnauthorisedAgentKey -> isUnauthorised.toString)
  }

}
