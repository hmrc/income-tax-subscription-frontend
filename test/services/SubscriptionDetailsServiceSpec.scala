/*
 * Copyright 2021 HM Revenue & Customs
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

import models.common.business.BusinessNameModel
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.BusinessName
import utilities.{TestModels, UnitTestTrait}

class SubscriptionDetailsServiceSpec extends UnitTestTrait
  with MockSubscriptionDetailsService {

  "mock Subscription Details  service" should {
    object TestSubscriptionDetails {
      val subscriptionDetailsService: SubscriptionDetailsService = MockSubscriptionDetailsService
    }

    "configure and verify fetch and save business name as specified" in {
      val testBusinessName = BusinessNameModel("my business name")
      setupMockSubscriptionDetailsSaveFunctions()
      mockFetchBusinessNameFromSubscriptionDetails(testBusinessName)

      val businessName = await(
        for {
          businessName <- TestSubscriptionDetails.subscriptionDetailsService.fetchBusinessName("test-reference")
          _ <- TestSubscriptionDetails.subscriptionDetailsService.saveBusinessName("test-reference", testBusinessName)
        } yield businessName
      )

      businessName shouldBe Some(testBusinessName)

      verifySubscriptionDetailsFetch(BusinessName, 2)
      verifySubscriptionDetailsSave(BusinessName, 1)
    }

    "configure and verify fetch all as specified" in {
      val testFetchAll = TestModels.emptyCacheMap
      mockFetchAllFromSubscriptionDetails(testFetchAll)

      val fetched = await(TestSubscriptionDetails.subscriptionDetailsService.fetchAll("test-reference"))
      fetched shouldBe testFetchAll

      verifySubscriptionDetailsFetchAll(1)
    }


  }

}
