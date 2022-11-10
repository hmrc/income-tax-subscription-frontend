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

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import models.DateModel
import models.common.business._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.MockIncomeTaxSubscriptionConnector
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.UnitTestTrait

class RemoveBusinessServiceSpec extends UnitTestTrait
  with MockIncomeTaxSubscriptionConnector {

  private val testReference = "reference"
  private val testBusinessId = "id"

  private def testBusiness(id: String) =
    SelfEmploymentData(
      id = id,
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel("123", Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )


  "mock RemoveBusinessService" must {

    object TestRemoveBusiness extends RemoveBusinessService(mockIncomeTaxSubscriptionConnector)

    "delete and save business details and accounting method" when {
      "a reference, business id and single selfEmploymentsData are passed into the service" in {
        mockSaveSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey, Seq.empty)(Right(PostSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(BusinessAccountingMethod)(Right(DeleteSubscriptionDetailsSuccessResponse))
        val result = await(TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id"))))

        result.isRight shouldBe true
        verifyDeleteSubscriptionDetails(BusinessAccountingMethod, 1)
      }
    }

    "delete and save business details but not accounting method" when {
      "a reference, business id and multiple selfEmploymentsData are passed into the service" in {
        mockSaveSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey, Seq(testBusiness("id1")))(Right(PostSubscriptionDetailsSuccessResponse))
        val result = await(TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id"), testBusiness("id1"))))

        result.isRight shouldBe true
        verifyDeleteSubscriptionDetails(BusinessAccountingMethod, 0)
      }
    }
  }
}
