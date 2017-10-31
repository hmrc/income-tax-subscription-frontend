/*
 * Copyright 2017 HM Revenue & Customs
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

package agent.controllers

import agent.audit.Logging
import agent.services.CacheUtil._
import agent.services.mocks._
import agent.utils.TestConstants._
import agent.utils.TestModels
import agent.utils.TestModels.testCacheMap
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService
  with MockClientRelationshipService
  with MockSubscriptionOrchestrationService {

  override def beforeEach(): Unit = {
    super.beforeEach()
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

    def call = TestCheckYourAnswersController.show(subscriptionRequest)

    "There are both a matched nino and terms in keystore" should {
      "return ok (200)" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)

        status(call) must be(Status.OK)
      }
    }

    "There are no a matched nino in keystore" should {
      s"return redirect ${agent.controllers.matching.routes.ConfirmClientController.show().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(matchedNino = None))
        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ConfirmClientController.show().url)
      }
    }

    "There is a matched nino but no terms in keystore" should {
      s"return redirect ${agent.controllers.routes.TermsController.showTerms().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.routes.TermsController.showTerms().url)
      }
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" when {
    lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

    def call: Future[Result] = TestCheckYourAnswersController.submit(request)

    "There are no a matched nino in keystore" should {
      s"return redirect ${agent.controllers.matching.routes.ConfirmClientController.show().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(matchedNino = None))
        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ConfirmClientController.show().url)
      }
    }

    "There is a matched nino but no terms in keystore" should {
      s"return redirect ${agent.controllers.routes.TermsController.showTerms().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.routes.TermsController.showTerms().url)
      }
    }

    "There are both a matched nino and terms in keystore and the submission is successful" should {
      lazy val result = call
      // generate a new nino specifically for this test,
      // since the default value in test constant may be used by accident
      lazy val newTestNino = new Generator().nextNino.nino

      // This is used to verify that the nino returned from client matching is used instead of whatever the user entered
      lazy val testSummary = TestModels.testCacheMapCustom(
        matchedNino = newTestNino,
        clientDetailsModel = TestModels.testClientDetails.copy(nino = testNino)
      )

      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystore(
          fetchAll = testSummary
        )
        mockCreateSubscriptionSuccess(testARN, newTestNino, testSummary.getSummary())
        setupCreateClientRelationship(testARN, testMTDID)

        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 1)

        //TODO - Test path header being sent to backend
        // verifySubscriptionHeader(ITSASessionKeys.RequestURI -> request.uri)
      }

      s"redirect to '${agent.controllers.routes.ConfirmationController.showConfirmation().url}'" in {
        redirectLocation(result) mustBe Some(agent.controllers.routes.ConfirmationController.showConfirmation().url)
      }
    }

    "When the submission is unsuccessful" should {
      "return a failure if subscription fails" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        mockCreateSubscriptionFailure(testARN, testNino, testCacheMap.getSummary())

        val ex = intercept[InternalServerException](await(call))
        ex.message mustBe "Successful response not received from submission"
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

      // TODO re-enable create relationship test once the agent team is ready
      "return a failure if create client relationship fails" ignore {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        mockCreateSubscriptionSuccess(testARN, testNino, testCacheMap.getSummary())

        setupCreateClientRelationshipFailure(testARN, testMTDID)(new Exception())

        val ex = intercept[InternalServerException](await(call))
        ex.message mustBe "Failed to create client relationship"
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

    }
  }

  "The back url" should {
    s"point to ${agent.controllers.routes.TermsController.showTerms().url}" in {
      TestCheckYourAnswersController.backUrl mustBe agent.controllers.routes.TermsController.showTerms().url
    }
  }

  authorisationTests()

}
