/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.subscription

import controllers.ControllerBaseSpec
import utilities.individual.TestConstants._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.MockKeystoreService
import uk.gov.hmrc.http.InternalServerException

class ClaimSubscriptionControllerSpec extends ControllerBaseSpec with MockKeystoreService with MockSubscriptionOrchestrationService {

  override val controllerName: String = "ClaimSubscriptionController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "claim" -> TestClaimSubscriptionController.claim()
  )

  object TestClaimSubscriptionController extends ClaimSubscriptionController(
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionOrchestrationService
  )

  "Calling the claim action of the ClaimSubscriptionController with a subscribed Authenticated User" should {
    "return a a redirect to the confirmation page" in {
      mockFetchSubscriptionIdFromKeyStore(testMTDID)
      mockEnrolAndRefreshSuccess(testMTDID, testNino)

      lazy val result = TestClaimSubscriptionController.claim(subscriptionRequest)

      status(result) must be(Status.OK)
    }

    "return an error where enrolment fails" in {
      mockFetchSubscriptionIdFromKeyStore(testMTDID)
      mockEnrolFailure(testMTDID, testNino)

      lazy val result = TestClaimSubscriptionController.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }

    "return an error where refresh profile fails" in {
      mockFetchSubscriptionIdFromKeyStore(testMTDID)
      mockRefreshFailure(testMTDID, testNino)

      lazy val result = TestClaimSubscriptionController.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }

    "return an error where keystore does not contain the MtditId" in {
      mockFetchSubscriptionIdFromKeyStore(fetchSubscriptionId = None)

      lazy val result = TestClaimSubscriptionController.claim(subscriptionRequest)

      intercept[InternalServerException](await(result))
    }
  }

  authorisationTests()
}
