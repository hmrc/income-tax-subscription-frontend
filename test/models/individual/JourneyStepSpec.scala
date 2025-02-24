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

import auth.individual.{ClaimEnrolment, SignUp}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.InternalServerException

class JourneyStepSpec extends PlaySpec {

  "JourneyStep.fromString" when {
    s"the key is provided as ${SignUp.name}" should {
      "return a sign up journey step" in {
        JourneyStep.fromString(SignUp.name) mustBe JourneyStep.SignUp
      }
    }
    s"the key is provided as ${ClaimEnrolment.name}" should {
      "return a pre sign up journey step" in {
        JourneyStep.fromString(ClaimEnrolment.name) mustBe JourneyStep.PreSignUp
      }
    }
    s"the key is provided as ${JourneyStep.PreSignUp}" should {
      "return a pre sign up journey step" in {
        JourneyStep.fromString(JourneyStep.PreSignUp.key) mustBe JourneyStep.PreSignUp
      }
    }
    s"the key is provided as ${JourneyStep.SignUp}" should {
      "return a sign up journey step" in {
        JourneyStep.fromString(JourneyStep.SignUp.key) mustBe JourneyStep.SignUp
      }
    }
    s"the key is provided as ${JourneyStep.Confirmation}" should {
      "return a confirmation journey step" in {
        JourneyStep.fromString(JourneyStep.Confirmation.key) mustBe JourneyStep.Confirmation
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
