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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import connectors.httpparser.{DeleteSubscriptionDetailsHttpParser, PostSubscriptionDetailsHttpParser}
import models.DateModel
import models.common.business._
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import utilities.{SubscriptionDataKeys, UnitTestTrait}

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

  object TestRemoveBusiness extends RemoveBusinessService(
    mockIncomeTaxSubscriptionConnector,
    mockSubscriptionDetailsService
  )

  "deleteBusiness" must {
    "remove the business from the businesses list and delete the sole trader businesses" when {
      "only a single business exists in the businesses list and is flagged for removal" in {
        mockSaveBusinesses(Seq.empty)(Right(PostSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey)(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result = TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id")))

        await(result) mustBe Right(DeleteSubscriptionDetailsSuccessResponse)
      }
    }
    "remove the business from the business list but don't delete anything" when {
      "the business to remove was not the only business" in {
        mockSaveBusinesses(Seq(testBusiness("id2")))(Right(PostSubscriptionDetailsSuccessResponse))

        val result = TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id"), testBusiness("id2")))

        await(result) mustBe Right(DeleteSubscriptionDetailsSuccessResponse)
      }
    }
    "return a failure response" when {
      "saving the businesses failed" in {
        mockSaveBusinesses(Seq.empty)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val result = TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id")))

        await(result) mustBe Left(RemoveBusinessService.SaveBusinessFailure)
      }

      "deleting the remnants of the businesses" in {
        mockSaveBusinesses(Seq.empty)(Right(PostSubscriptionDetailsSuccessResponse))
        mockDeleteSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey)(
          Left(DeleteSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result = TestRemoveBusiness.deleteBusiness(testReference, testBusinessId, Seq(testBusiness("id")))

        await(result) mustBe Left(RemoveBusinessService.DeleteBusinessesFailure)
      }
    }
  }
}
