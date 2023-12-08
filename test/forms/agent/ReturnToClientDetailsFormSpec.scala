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

package forms.agent

import forms.agent.ReturnToClientDetailsForm._
import models.ReturnToClientDetailsModel.{ContinueWithCurrentClient, SignUpAnotherClient}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.FormError

class ReturnToClientDetailsFormSpec extends PlaySpec with GuiceOneAppPerTest {

  "The ReturnToClientDetailsForm" should {
    "transform the request to the form case class" when {
      "Continue with current client is submitted" in {
        val testInput = Map(ReturnToClientDetailsForm.returnToClientDetails -> ContinueWithCurrentClient.key)
        val expected = ContinueWithCurrentClient
        val actual = returnToClientDetailsForm.bind(testInput)
        actual.errors mustBe Seq.empty[FormError]
        actual.value mustBe Some(expected)
      }
      "Sign Up Another Client is submitted" in {
        val testInput = Map(ReturnToClientDetailsForm.returnToClientDetails -> SignUpAnotherClient.key)
        val expected = SignUpAnotherClient
        val actual = returnToClientDetailsForm.bind(testInput)
        actual.errors mustBe Seq.empty[FormError]
        actual.value mustBe Some(expected)
      }
    }
    "Produce an error" when {
      "no input is provided" in {
        val testInput = Map.empty[String, String]
        val actual = returnToClientDetailsForm.bind(testInput)
        actual.value mustBe None
        actual.errors mustBe Seq(FormError(ReturnToClientDetailsForm.returnToClientDetails, "agent.return-to-client-details.error.empty"))
      }
    }
  }
}
