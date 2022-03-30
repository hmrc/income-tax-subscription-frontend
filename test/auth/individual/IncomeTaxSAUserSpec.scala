/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import utilities.ITSASessionKeys
import utilities.individual.Constants
import utilities.individual.TestConstants.{testCredId, testNino, testUtr}

class IncomeTaxSAUserSpec extends PlaySpec with GuiceOneServerPerTest {

  "IncomeTaxSAUser" when {
    "Nino and UTR are retrieved from auth" should {
      implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

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
        confidenceLevel,
        ""
      )

      s"have the expected NINO $testNino" in {
        user.nino mustBe Some(testNino)
      }

      s"have the expected UTR $testUtr" in {
        user.utr mustBe Some(testUtr)
      }

      s"have the confidence level of $confidenceLevel" in {
        user.confidenceLevel mustBe confidenceLevel
      }
    }

    "Nino and UTR are stored in session after being pulled from CID" should {
      val confidenceLevel = ConfidenceLevel.L50

      implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
        ITSASessionKeys.NINO -> testNino,
        ITSASessionKeys.UTR -> testUtr
      )

      lazy val user = IncomeTaxSAUser(
        Enrolments(Set.empty),
        None,
        None,
        confidenceLevel,
        ""
      )

      s"have the expected NINO $testNino" in {
        user.nino mustBe Some(testNino)
      }

      s"have the expected UTR $testUtr" in {
        user.utr mustBe Some(testUtr)
      }

      "have the default confidence level of 50" in {
        user.confidenceLevel mustBe ConfidenceLevel.L50
      }
    }


    def user(credentialRole: Option[CredentialRole]): IncomeTaxSAUser =
      IncomeTaxSAUser(
        Enrolments(Set.empty),
        None,
        credentialRole,
        L50,
        testCredId
      )

    "role is Admin" should {
      "return false for isAssistant" in {
        user(Some(User)).isAssistant mustBe false
      }
    }

    "role is User" should {
      "return false for isAssistant" in {
        user(Some(User)).isAssistant mustBe false
      }
    }
    "role is Assistant" should {
      "return true for isAssistant" in {
        user(Some(Assistant)).isAssistant mustBe true
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
