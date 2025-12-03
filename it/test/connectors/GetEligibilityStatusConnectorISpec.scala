/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.individual.eligibility.GetEligibilityStatusConnector
import helpers.ComponentSpecBase
import helpers.servicemocks.EligibilityStub.{stubEligibilityResponseBoth, stubEligibilityResponseError, stubEligibilityResponseInvalid}
import models.EligibilityStatus
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.HttpResult.HttpResult

import scala.concurrent.Future

class GetEligibilityStatusConnectorISpec extends ComponentSpecBase {

  val connector: GetEligibilityStatusConnector = app.injector.instanceOf[GetEligibilityStatusConnector]
  val nino: String = "test-nino"
  val utr: String = "test-utr"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getEligibilityStatus(sautr)" must {
    "return an eligibility status" when {
      "receiving an OK status with valid json" in {
        stubEligibilityResponseBoth(utr)(currentYearResponse = true, nextYearResponse = true, exemptionReason= None)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(utr)

        await(result) mustBe Right(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason= None))
      }
    }
    "return a http connector error response" when {
      "receiving an OK status with invalid json" in {
        stubEligibilityResponseInvalid(utr)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(utr)

        await(result).isLeft mustBe true
      }
      "receiving a non OK status" in {
        stubEligibilityResponseError(utr)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(utr)

        await(result).isLeft mustBe true
      }
    }
  }

  "getEligibilityStatus(nino, utr)" must {
    "return an eligibility status" when {
      "receiving an OK status with valid json" in {
        stubEligibilityResponseBoth(nino, utr)(currentYearResponse = true, nextYearResponse = true)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(nino, utr)

        await(result) mustBe Right(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason= None))
      }
    }
    "return a http connector error response" when {
      "receiving an OK status with invalid json" in {
        stubEligibilityResponseInvalid(nino, utr)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(nino, utr)

        await(result).isLeft mustBe true
      }
      "receiving a non OK status" in {
        stubEligibilityResponseError(nino, utr)

        val result: Future[HttpResult[EligibilityStatus]] = connector.getEligibilityStatus(nino, utr)

        await(result).isLeft mustBe true
      }
    }
  }

}
