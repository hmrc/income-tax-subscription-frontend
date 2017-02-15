/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import enums.IdentityVerificationResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import testHelpers.MockIdentityVerificationConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class IdentityVerificationConnectorSpec extends UnitSpec with WithFakeApplication with MockitoSugar with ScalaFutures {
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "IdentityVerificationConnector" should {

//    "use the WSHttp" in {
//      IdentityVerificationConnector.http shouldBe WSHttp
//    }

    "return success when identityVerification returns success" in {
      MockIdentityVerificationConnector.identityVerificationResponse("success-journey-id").futureValue shouldBe IdentityVerificationResult.Success
    }

    "return incomplete when identityVerification returns incomplete" in {
      MockIdentityVerificationConnector.identityVerificationResponse("incomplete-journey-id").futureValue shouldBe IdentityVerificationResult.Incomplete
    }

    "return failed matching when identityVerification returns failed matching" in {
      MockIdentityVerificationConnector.identityVerificationResponse("failed-matching-journey-id").futureValue shouldBe IdentityVerificationResult.FailedMatching
    }

    "return insufficient evidence when identityVerification returns insufficient evidence" in {
      MockIdentityVerificationConnector.identityVerificationResponse("insufficient-evidence-journey-id").futureValue shouldBe IdentityVerificationResult.InsufficientEvidence
    }

    "return locked out when identityVerification returns locked out" in {
      MockIdentityVerificationConnector.identityVerificationResponse("locked-out-journey-id").futureValue shouldBe IdentityVerificationResult.LockedOut
    }

    "return user aborted when identityVerification returns user aborted" in {
      MockIdentityVerificationConnector.identityVerificationResponse("user-aborted-journey-id").futureValue shouldBe IdentityVerificationResult.UserAborted
    }

    "return timeout when identityVerification returns timeout" in {
      MockIdentityVerificationConnector.identityVerificationResponse("timeout-journey-id").futureValue shouldBe IdentityVerificationResult.Timeout
    }

    "return technical issue when identityVerification returns technical issue" in {
      MockIdentityVerificationConnector.identityVerificationResponse("technical-issue-journey-id").futureValue shouldBe IdentityVerificationResult.TechnicalIssue
    }

    "return precondition failed when identityVerification returns precondition failed" in {
      MockIdentityVerificationConnector.identityVerificationResponse("precondition-failed-journey-id").futureValue shouldBe IdentityVerificationResult.PreconditionFailed
    }

    "return failed IV when identityVerification returns failed IV result type" in {
      MockIdentityVerificationConnector.identityVerificationResponse("failed-iv-journey-id").futureValue shouldBe IdentityVerificationResult.FailedIV
    }

    "return unknown outcome when identityVerification returns non-existent result type" in {
      MockIdentityVerificationConnector.identityVerificationResponse("invalid-journey-id").futureValue shouldBe IdentityVerificationResult.UnknownOutcome
    }

    "return failed future for invalid json fields" in {
      val result = MockIdentityVerificationConnector.identityVerificationResponse("invalid-fields-journey-id")
      ScalaFutures.whenReady(result.failed) { e =>
        e shouldBe a[JsonValidationException]
      }
    }
  }
}
