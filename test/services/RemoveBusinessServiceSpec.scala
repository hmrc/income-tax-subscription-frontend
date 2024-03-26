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

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import models.common.SoleTraderBusinesses
import models.common.business._
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey
import utilities.UnitTestTrait

class RemoveBusinessServiceSpec extends UnitTestTrait
  with MockIncomeTaxSubscriptionConnector with MockSubscriptionDetailsService {

  private val testReference = "reference"
  private val testBusinessId = "id"

  private def testBusiness(id: String) =
    SelfEmploymentData(
      id = id,
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )

  "mock RemoveBusinessService" must {

    object TestRemoveBusiness extends RemoveBusinessService(mockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService)

    "delete and save business details and accounting method" when {
      "a reference, business id and single selfEmploymentsData are passed into the service" in {
        mockSaveBusinesses(testReference)
        mockDeleteSubscriptionDetails(SoleTraderBusinessesKey)(Right(DeleteSubscriptionDetailsSuccessResponse))
        mockDeleteIncomeSourceConfirmationSuccess()
        val result = await(TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id")), Some(Cash)))

        result.isRight shouldBe true
        verifyDeleteSubscriptionDetails(SoleTraderBusinessesKey, 1)
        verifySaveBusinesses(1, testReference)
      }
    }

    "delete and save business details but not accounting method" when {
      "a reference, business id and multiple selfEmploymentsData are passed into the service" in {
        mockSaveBusinesses(testReference)
        mockDeleteIncomeSourceConfirmationSuccess()
        val result = await(TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id"), testBusiness("id1")), Some(Cash)))

        result.isRight shouldBe true
        verifyDeleteSubscriptionDetails(SoleTraderBusinessesKey, 0)
        verify(mockConnector, times(1)).saveSubscriptionDetails[SoleTraderBusinesses](
          ArgumentMatchers.eq(testReference),
          ArgumentMatchers.eq(SoleTraderBusinessesKey),
          ArgumentMatchers.eq(SoleTraderBusinesses(Seq(testBusiness("id1").toSoleTraderBusiness), Some(Cash)))
        )(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }
}
