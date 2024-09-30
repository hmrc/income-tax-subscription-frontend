/*
 * Copyright 2024 HM Revenue & Customs
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

package models.agent

import auth.agent.{AgentSignUp, AgentUserMatching}
import play.api.Logging
import uk.gov.hmrc.http.InternalServerException

sealed trait JourneyStep {
  val key: String
}

object JourneyStep extends Logging {

  case object ClientDetails extends JourneyStep {
    val key = "ClientDetails"
  }

  case object SignPosted extends JourneyStep {
    val key = "SignPosted"
  }

  case object ConfirmedClient extends JourneyStep {
    val key = "ConfirmedClient"
  }

  case object Confirmation extends JourneyStep {
    val key = "Confirmation"
  }

  //scalastyle:off
  def fromString(key: String, clientDetailsConfirmed: Boolean, hasMtditid: Boolean): JourneyStep = {
    key match {

      // if user has old mtditid session key, they are in a confirmation state
      case _ if hasMtditid =>
        logger.info("[Agent][JourneyStep] - old journey state used in new journey system")
        Confirmation

      // if the user is in the old user matching state with client details confirmed, they are sign posted
      case AgentUserMatching.name if clientDetailsConfirmed =>
        logger.info("[Agent][JourneyStep] - old journey state used in new journey system")
        SignPosted

      // if the user is in the old user matching state without client details confirmed, they are in the enter client details section
      case AgentUserMatching.name =>
        logger.info("[Agent][JourneyStep] - old journey state used in new journey system")
        ClientDetails

      // if the user has the old sign up state, treat that as a confirmed client state
      case AgentSignUp.name =>
        logger.info("[Agent][JourneyStep] - old journey state used in new journey system")
        ConfirmedClient

      case ClientDetails.key => ClientDetails
      case SignPosted.key => SignPosted
      case ConfirmedClient.key => ConfirmedClient
      case Confirmation.key => Confirmation
      case _ => throw new InternalServerException(s"[Agent][JourneyStep] - Unsupported journey key - $key")
    }
  }
  //scalastyle:on

}