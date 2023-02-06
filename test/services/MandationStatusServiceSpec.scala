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

package services

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockMandationStatusConnector, MockSubscriptionDetailsService}

class MandationStatusServiceSpec extends MockSubscriptionDetailsService with MockMandationStatusConnector {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  object TestMandationStatusService extends MandationStatusService(mockMandationStatusConnector, MockSubscriptionDetailsService)

  "retrieveMandationStatus" should {
    "retrieve and save the mandation status" when {
      "the backend returns a successful response" in {
        mockGetMandationStatus()
        mockSaveMandationStatus("test-reference")

        await(TestMandationStatusService.copyMandationStatus("test-reference", "test-nino", "test-utr"))

        verifySaveMandationStatus(1, "test-reference")
      }
    }

    "do nothing" when {
      "the backend returns a failure" in {
        mockFailedGetMandationStatus()

        await(TestMandationStatusService.copyMandationStatus("test-reference", "test-nino", "test-utr"))

        verifySaveMandationStatus(0, "test-reference")
      }
    }
  }
}
