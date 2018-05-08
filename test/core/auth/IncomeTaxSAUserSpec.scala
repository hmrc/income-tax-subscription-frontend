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

import core.utils.TestConstants.{testNino, testUtr}
import core.{Constants, ITSASessionKeys}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IncomeTaxSAUserSpec extends UnitSpec with WithFakeApplication {

  "IncomeTaxSAUser" when {
    "Nino and UTR are retrieved from auth" should {
      implicit lazy val request = FakeRequest()

      val confidenceLevel = L50

      lazy val user = IncomeTaxSAUser(
        Enrolments(
          Set(
            Enrolment(Constants.ninoEnrolmentName,
              Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, testNino)),
              "Activated"
            ),
            Enrolment(Constants.utrEnrolmentName,
              Seq(EnrolmentIdentifier(Constants.utrEnrolmentIdentifierKey, testUtr)),
              "Activated"
            )
          )
        ),
        None,
        None,
        confidenceLevel
      )

      s"have the expected NINO $testNino" in {
        user.nino shouldBe Some(testNino)
      }

      s"have the expected UTR $testUtr" in {
        user.utr shouldBe Some(testUtr)
      }

      s"have the confidence level of $confidenceLevel" in {
        user.confidenceLevel shouldBe confidenceLevel
      }
    }

    "Nino and UTR are stored in session after being pulled from CID" should {
      val confidenceLevel = ConfidenceLevel.L0

      implicit lazy val request = FakeRequest().withSession(
        ITSASessionKeys.NINO -> testNino,
        ITSASessionKeys.UTR -> testUtr
      )

      lazy val user = IncomeTaxSAUser(
        Enrolments(Set.empty),
        None,
        None,
        confidenceLevel
      )

      s"have the expected NINO $testNino" in {
        user.nino shouldBe Some(testNino)
      }

      s"have the expected UTR $testUtr" in {
        user.utr shouldBe Some(testUtr)
      }

      "have the default confidence level of 0" in {
        user.confidenceLevel shouldBe ConfidenceLevel.L0
      }
    }


    def user(credentialRole: Option[CredentialRole]) =
      IncomeTaxSAUser(
        Enrolments(Set.empty),
        None,
        credentialRole,
        L50
      )

    "role is Admin" should {
      "return false for isAssistant" in {
        user(Some(Admin)).isAssistant shouldBe false
      }
    }

    "role is User" should {
      "return false for isAssistant" in {
        user(Some(User)).isAssistant shouldBe false
      }
    }
    "role is Assistant" should {
      "return true for isAssistant" in {
        user(Some(Assistant)).isAssistant shouldBe true
      }
    }
    "role is None" should {
      "throws IllegalArgumentException for isAssistant" in {
        intercept[IllegalArgumentException] {
          user(None).isAssistant
        }
      }
    }
  }
}
