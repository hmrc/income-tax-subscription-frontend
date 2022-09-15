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

package services

import models.audits.MandationStatusAuditing.MandationStatusAuditModel
import models.status.MandationStatus.Voluntary
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockAuditingService, MockMandationStatusConnector, MockSubscriptionDetailsService}
import utilities.AccountingPeriodUtil

class MandationStatusServiceSpec extends MockSubscriptionDetailsService with MockMandationStatusConnector with MockAuditingService {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  object TestMandationStatusService extends MandationStatusService(mockMandationStatusConnector, MockSubscriptionDetailsService, mockAuditingService)

  "retrieveMandationStatus" should {
    "retrieve and save the mandation status" when {
      "the backend returns a successful response" in {
        mockGetMandationStatus()
        mockSaveMandationStatus("test-reference")

        await(TestMandationStatusService.retrieveMandationStatus("test-reference", "test-user-type", "test-nino", "test-utr"))

        verifySaveMandationStatus(1, "test-reference")
        verifyAudit(MandationStatusAuditModel(
          userType = "test-user-type",
          agentReferenceNumber = None,
          utr = "test-utr",
          nino = "test-nino",
          currentYear = AccountingPeriodUtil.getCurrentTaxYear.toShortTaxYear,
          currentYearStatus = Voluntary.value,
          nextYear = AccountingPeriodUtil.getNextTaxYear.toShortTaxYear,
          nextYearStatus = Voluntary.value
        ))
      }
    }

    "do nothing" when {
      "the backend returns a failure" in {
        mockFailedGetMandationStatus()

        await(TestMandationStatusService.retrieveMandationStatus("test-reference", "test-user-type", "test-nino", "test-utr"))

        verifySaveMandationStatus(0, "test-reference")
      }
    }
  }
}
