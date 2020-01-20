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

import agent.audit.Logging
import agent.services.CacheUtil._
import agent.services.mocks._
import agent.utils.TestConstants.{testNino, _}
import agent.utils.TestModels
import agent.utils.TestModels.testCacheMap
import core.config.featureswitch.{AgentPropertyCashOrAccruals, EligibilityPagesFeature, FeatureSwitching}
import incometax.subscription.models.{Both, Business, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockClientRelationshipService
  with MockSubscriptionOrchestrationService
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
    disable(AgentPropertyCashOrAccruals)
  }

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    subscriptionService = mockSubscriptionOrchestrationService,
    clientRelationshipService = mockClientRelationshipService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  "Calling the show action of the CheckYourAnswersController with an authorised user" when {

    def call(request: Request[AnyContent] = subscriptionRequest): Future[Result] = TestCheckYourAnswersController.show(request)

    "There are both a matched nino and terms in keystore" should {
      "return ok (200)" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap, fetchIncomeSource = Business)

        status(call()) must be(Status.OK)
      }
    }

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)

        val result = call(subscriptionRequest.removeFromSession(ITSASessionKeys.NINO))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }

    "When the terms have not been agreed" should {
      "redirect back to Terms if there is no terms in keystore" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call()

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.TermsController.show().url)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

      "redirect back to Terms if there is terms is set to false in keystore" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = Some(false)))

        val result = call()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) must contain(controllers.agent.routes.TermsController.show(editMode = true).url)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }
    }

    "When the terms have not been agreed" should {
      "show the CYA page if there is no terms in keystore when the eligibility pages feature switch is enabled" in {
        enable(EligibilityPagesFeature)
        setupMockKeystore(
          fetchIncomeSource = Business,
          fetchAll = TestModels.testCacheMapCustom(terms = None)
        )

        status(call()) must be(Status.OK)
      }

      "show the CYA page if there is terms is set to false in keystore when the eligibility pages feature switch is enabled" in {
        enable(EligibilityPagesFeature)
        setupMockKeystore(
          fetchIncomeSource = Business,
          fetchAll = TestModels.testCacheMapCustom(terms = None)
        )

        status(call()) must be(Status.OK)
      }
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" when {
    lazy val testSummary = TestModels.testCacheMap

    def call(request: Request[AnyContent]): Future[Result] = TestCheckYourAnswersController.submit(request)

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN).removeFromSession(ITSASessionKeys.NINO)

        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }

    "There is a matched nino but no terms in keystore" should {
      s"return redirect ${controllers.agent.routes.TermsController.show().url}" in {
        lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.TermsController.show().url)
      }
    }

    "There is a matched nino but no terms in keystore" should {
      s"return redirect ${controllers.agent.routes.TermsController.show().url} when the eligibility pages feature switch is enabled" should {
        lazy val newTestNino = new Generator().nextNino.nino
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN, ITSASessionKeys.NINO -> newTestNino)
        lazy val result = call(authorisedAgentRequest)
        lazy val testSummary = TestModels.testCacheMapCustom(terms = None)

        "return a redirect status (SEE_OTHER - 303)" in {
          enable(EligibilityPagesFeature)
          setupMockKeystore(
            fetchIncomeSource = Business,
            fetchAll = TestModels.testCacheMapCustom(terms = None)
          )
          mockCreateSubscriptionSuccess(testARN, newTestNino, testSummary.getSummary())

          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)
        }

        s"redirect to '${controllers.agent.routes.ConfirmationController.show().url}'" in {
          enable(EligibilityPagesFeature)
          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationController.show().url)
        }
      }
    }


    "The agent is authorised and" should {
      "There are both a matched nino and terms in keystore and the submission is successful" should {
        // generate a new nino specifically for this test,
        // since the default value in test constant may be used by accident
        lazy val newTestNino = new Generator().nextNino.nino
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN, ITSASessionKeys.NINO -> newTestNino)

        lazy val result = call(authorisedAgentRequest)

        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(
            fetchAll = testSummary
          )
          mockCreateSubscriptionSuccess(testARN, newTestNino, testSummary.getSummary())

          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)

          //TODO - Test path header being sent to backend
          // verifySubscriptionHeader(ITSASessionKeys.RequestURI -> request.uri)
        }

        s"redirect to '${controllers.agent.routes.ConfirmationController.show().url}'" in {
          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationController.show().url)
        }
      }

      "When the submission is unsuccessful" should {
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN, ITSASessionKeys.NINO -> testNino)

        "return a failure if subscription fails" in {
          setupMockKeystore(fetchAll = TestModels.testCacheMap)
          mockCreateSubscriptionFailure(testARN, testNino, TestModels.testCacheMap.getSummary())

          val ex = intercept[InternalServerException](await(call(authorisedAgentRequest)))
          ex.message mustBe "Successful response not received from submission"
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
        }

        // TODO re-enable create relationship test once the agent team is ready
        "return a failure if create client relationship fails" ignore {
          val request = authorisedAgentRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

          setupMockKeystore(fetchAll = TestModels.testCacheMap)
          mockCreateSubscriptionSuccess(testARN, testNino, testCacheMap.getSummary())

          val ex = intercept[InternalServerException](await(call(request)))
          ex.message mustBe "Failed to create client relationship"
          verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
        }
      }
    }
  }

  "The back url" should {
    s"point to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url}" when {
      "the property cash/accruals and the eligibility pages feature switches is enabled" when {
        "on the property only journey" in {
          enable(AgentPropertyCashOrAccruals)
          enable(EligibilityPagesFeature)
          TestCheckYourAnswersController.backUrl(Some(Property))(fakeRequest) mustBe business.routes.PropertyAccountingMethodController.show().url
        }
        "on the property and business journey" in {
          enable(AgentPropertyCashOrAccruals)
          enable(EligibilityPagesFeature)
          TestCheckYourAnswersController.backUrl(Some(Both))(fakeRequest) mustBe business.routes.PropertyAccountingMethodController.show().url
        }
      }
    }

    s"point to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url}" when {
      "on the business journey" in {
        enable(EligibilityPagesFeature)
        TestCheckYourAnswersController.backUrl(Some(Business))(fakeRequest) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
      }
      "on the both journey" in {
        enable(EligibilityPagesFeature)
        TestCheckYourAnswersController.backUrl(Some(Both))(fakeRequest) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
      }
    }

    s"point to ${controllers.agent.routes.IncomeSourceController.show().url} on the property journey" in {
      enable(EligibilityPagesFeature)
      TestCheckYourAnswersController.backUrl(Some(Property))(fakeRequest) mustBe controllers.agent.routes.IncomeSourceController.show().url
    }

    s"point to ${controllers.agent.routes.TermsController.show().url} when the eligibility pages feature switch is disabled" in {
      TestCheckYourAnswersController.backUrl(None)(fakeRequest) mustBe controllers.agent.routes.TermsController.show().url
    }

  }

  authorisationTests()

}
