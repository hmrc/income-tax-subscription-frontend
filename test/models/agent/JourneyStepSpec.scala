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
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.InternalServerException

class JourneyStepSpec extends PlaySpec {

  "JourneyStep.fromString" when {
    "hasMtditid is provided as true" should {
      "return a Confirmation journey step" in {
        JourneyStep.fromString("", clientDetailsConfirmed = false, hasMtditid = true) mustBe JourneyStep.Confirmation
      }
    }
    s"the key is provided as ${AgentUserMatching.name} and clientDetailsConfirmed is set to true" should {
      "return a SignPosted journey step" in {
        JourneyStep.fromString(AgentUserMatching.name, clientDetailsConfirmed = true, hasMtditid = false) mustBe JourneyStep.SignPosted
      }
    }
    s"the key is provided as ${AgentUserMatching.name} and clientDetailsConfirmed is set to false" should {
      "return a ClientDetails journey step" in {
        JourneyStep.fromString(AgentUserMatching.name, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.ClientDetails
      }
    }
    s"the key is provided as ${AgentSignUp.name}" should {
      "return a ConfirmedClient journey step" in {
        JourneyStep.fromString(AgentSignUp.name, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.ConfirmedClient
      }
    }
    s"the key is provided as ${JourneyStep.ClientDetails.key}" should {
      "return a ClientDetails journey step" in {
        JourneyStep.fromString(JourneyStep.ClientDetails.key, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.ClientDetails
      }
    }
    s"the key is provided as ${JourneyStep.SignPosted.key}" should {
      "return a SignPosted journey step" in {
        JourneyStep.fromString(JourneyStep.SignPosted.key, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.SignPosted
      }
    }
    s"the key is provided as ${JourneyStep.ConfirmedClient.key}" should {
      "return a ConfirmedClient journey step" in {
        JourneyStep.fromString(JourneyStep.ConfirmedClient.key, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.ConfirmedClient
      }
    }
    s"the key is provided as ${JourneyStep.Confirmation.key}" should {
      "return a Confirmation journey step" in {
        JourneyStep.fromString(JourneyStep.Confirmation.key, clientDetailsConfirmed = false, hasMtditid = false) mustBe JourneyStep.Confirmation
      }
    }
    s"the key is provided as anything else and hasMtditid is provided as false" should {
      "throw an InternalServerException" in {
        intercept[InternalServerException](JourneyStep.fromString("other", clientDetailsConfirmed = false, hasMtditid = false))
          .message mustBe "[Agent][JourneyStep] - Unsupported journey key - other"
      }
    }
  }

}
