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

package controllers.agent


import models.individual.subscription.{Both, Business, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import services.agent.mocks._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.CacheUtil._
import utilities.agent.TestConstants.{testNino, _}
import utilities.agent.TestModels
import utilities.agent.TestModels.testCacheMap

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockClientRelationshipService
  with MockSubscriptionOrchestrationService {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    mockAuthService,
    MockKeystoreService,
    mockSubscriptionOrchestrationService
  )

  "Calling the show action of the CheckYourAnswersController with an authorised user" when {

    def call(request: Request[AnyContent] = subscriptionRequest): Future[Result] = TestCheckYourAnswersController.show(request)

    "There are both a matched nino and terms in keystore" should {
      "return ok (200)" in {
        mockFetchIncomeSourceFromKeyStore(Business)
        mockFetchAllFromKeyStore(TestModels.testCacheMap)

        status(call()) must be(Status.OK)
      }
    }

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        mockFetchAllFromKeyStore(TestModels.testCacheMap)

        val result = call(subscriptionRequest.removeFromSession(ITSASessionKeys.NINO))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" when {
    lazy val testSummary = TestModels.testCacheMap

    def call(request: Request[AnyContent]): Future[Result] = TestCheckYourAnswersController.submit(request)

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN).removeFromSession(ITSASessionKeys.NINO)

        mockFetchAllFromKeyStore(TestModels.testCacheMap)
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }

    "The agent is authorised and" should {
      "There is a matched nino and utr in keystore and the submission is successful" should {
        // generate a new nino specifically for this test,
        // since the default value in test constant may be used by accident
        lazy val newTestNino = new Generator().nextNino.nino
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(
          ITSASessionKeys.ArnKey -> testARN,
          ITSASessionKeys.NINO -> newTestNino,
          ITSASessionKeys.UTR -> testUtr
        )

        lazy val result = call(authorisedAgentRequest)

        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions
          mockFetchAllFromKeyStore(testSummary)

          mockCreateSubscriptionSuccess(testARN, newTestNino, testUtr, testSummary.getSummary())

          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)
        }

        s"redirect to '${controllers.agent.routes.ConfirmationController.show().url}'" in {
          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationController.show().url)
        }
      }

      "When the submission is unsuccessful" should {
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(
          ITSASessionKeys.ArnKey -> testARN,
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.UTR -> testUtr
        )

        "return a failure if subscription fails" in {
          mockFetchAllFromKeyStore(TestModels.testCacheMap)
          mockCreateSubscriptionFailure(testARN, testNino, testUtr, TestModels.testCacheMap.getSummary())

          val ex = intercept[InternalServerException](await(call(authorisedAgentRequest)))
          ex.message mustBe "Successful response not received from submission"
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
        }
        "return a failure if create client relationship fails" ignore {
          val request = authorisedAgentRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

          mockFetchAllFromKeyStore(TestModels.testCacheMap)
          mockCreateSubscriptionSuccess(testARN, testNino, testUtr, testCacheMap.getSummary())

          val ex = intercept[InternalServerException](await(call(request)))
          ex.message mustBe "Failed to create client relationship"
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
        }
      }
    }
  }

  "The back url" should {
    s"point to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url}" when {
      "on the property only journey" in {
        TestCheckYourAnswersController.backUrl(Some(Property))(fakeRequest) mustBe business.routes.PropertyAccountingMethodController.show().url
      }
      "on the property and business journey" in {
        TestCheckYourAnswersController.backUrl(Some(Both))(fakeRequest) mustBe business.routes.PropertyAccountingMethodController.show().url
      }
    }

    s"point to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url}" when {
      "on the business only journey" in {
        TestCheckYourAnswersController.backUrl(Some(Business))(fakeRequest) mustBe
          controllers.agent.business.routes.BusinessAccountingMethodController.show().url
      }
    }

    s"point to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url} on the property journey" in {
      TestCheckYourAnswersController.backUrl(Some(Property))(fakeRequest) mustBe controllers.agent.business.routes.PropertyAccountingMethodController.show().url
    }

  }

  authorisationTests()

}
