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

import models.agent.JourneyStep.*
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.InternalServerException

class JourneyStepSpec extends PlaySpec {

  "JourneyStep.fromString" when {

    val data = Map(
      UserMatching.key -> ClientDetails,
      SignUp.key -> ConfirmedClient,
      ClientDetails.key -> ClientDetails,
      SignPosted.key -> SignPosted,
      ConfirmedClient.key -> ConfirmedClient,
      Confirmation.key -> Confirmation,
    )

    "hasMtditid is provided as true" should {
      "return a Confirmation journey step" in {
        JourneyStep.fromString("", hasMtditid = true) mustBe JourneyStep.Confirmation
      }
    }

    data.foreach { case (key, expected) =>
      s"return $expected when the key is $key and hasMtditid is false" in {
        JourneyStep.fromString(key, hasMtditid = false) mustBe expected
      }
    }
    "throw an InternalServerException" in {
      intercept[InternalServerException](JourneyStep.fromString("other", hasMtditid = false))
        .message mustBe "[Agent][JourneyStep] - Unsupported journey key - other"
    }
  }

}
