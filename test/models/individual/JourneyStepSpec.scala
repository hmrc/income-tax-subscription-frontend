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

package models.individual

import models.individual.JourneyStep.{ClaimEnrolment, Confirmation, PreSignUp, SignUp}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.InternalServerException

class JourneyStepSpec extends PlaySpec {

  "JourneyStep.fromString" when {

    val data = Map(
      PreSignUp.key -> PreSignUp,
      SignUp.key -> SignUp,
      ClaimEnrolment.key -> ClaimEnrolment,
      Confirmation.key -> Confirmation
    )

    data.foreach { case (key, expected) =>
      s"the key is provided as $key" should {
        "return a pre sign up journey step" in {
          JourneyStep.fromString(key) mustBe expected
        }
      }
    }
    s"the key is provided as anything else" should {
      "throw an InternalServerException" in {
        intercept[InternalServerException](JourneyStep.fromString("other"))
          .message mustBe "[Individual][JourneyStep] - Unsupported journey key - other"
      }
    }
  }
}
