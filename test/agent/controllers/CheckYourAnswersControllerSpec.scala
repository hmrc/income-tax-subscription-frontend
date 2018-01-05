/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, Request, Result}
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

    def call(request: Request[AnyContent] = subscriptionRequest): Future[Result] = TestCheckYourAnswersController.show(request)

    "There are both a matched nino and terms in keystore" should {
      "return ok (200)" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)

        status(call()) must be(Status.OK)
      }
    }

    "There are no a matched nino in session" should {
      s"return redirect ${agent.controllers.matching.routes.ConfirmClientController.show().url}" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMap)

        val result = call(subscriptionRequest.removeFromSession(ITSASessionKeys.NINO))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ConfirmClientController.show().url)
      }
    }

    "When the terms have not been agreed" should {

      "redirect back to Terms if there is no terms in keystore" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call()

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.routes.TermsController.show().url)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

      "redirect back to Terms if there is terms is set to false in keystore" in {
        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = Some(false)))

        val result = call()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) must contain(agent.controllers.routes.TermsController.show(editMode = true).url)
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" when {

    def call(request: Request[AnyContent]): Future[Result] = TestCheckYourAnswersController.submit(request)

    "There are no a matched nino in session" should {
      s"return redirect ${agent.controllers.matching.routes.ConfirmClientController.show().url}" in {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN).removeFromSession(ITSASessionKeys.NINO)

        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ConfirmClientController.show().url)
      }
    }

    "There is a matched nino but no terms in keystore" should {
      s"return redirect ${agent.controllers.routes.TermsController.show().url}" in {
        lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

        setupMockKeystore(fetchAll = TestModels.testCacheMapCustom(terms = None))
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(agent.controllers.routes.TermsController.show().url)
      }
    }

    "There are both a matched nino and terms in keystore and the submission is successful" should {
      // generate a new nino specifically for this test,
      // since the default value in test constant may be used by accident
      lazy val newTestNino = new Generator().nextNino.nino
      lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN, ITSASessionKeys.NINO -> newTestNino)

      lazy val result = call(request)

      lazy val testSummary = TestModels.testCacheMap

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

      s"redirect to '${agent.controllers.routes.ConfirmationController.show().url}'" in {
        redirectLocation(result) mustBe Some(agent.controllers.routes.ConfirmationController.show().url)
      }
    }

    "When the submission is unsuccessful" should {
      "return a failure if subscription fails" in {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        mockCreateSubscriptionFailure(testARN, testNino, testCacheMap.getSummary())

        val ex = intercept[InternalServerException](await(call(request)))
        ex.message mustBe "Successful response not received from submission"
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

      // TODO re-enable create relationship test once the agent team is ready
      "return a failure if create client relationship fails" ignore {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

        setupMockKeystore(fetchAll = TestModels.testCacheMap)
        mockCreateSubscriptionSuccess(testARN, testNino, testCacheMap.getSummary())

        val ex = intercept[InternalServerException](await(call(request)))
        ex.message mustBe "Failed to create client relationship"
        verifyKeystore(fetchAll = 1, saveSubscriptionId = 0)
      }

    }
  }

  "The back url" should {
    s"point to ${agent.controllers.routes.TermsController.show().url}" in {
      TestCheckYourAnswersController.backUrl mustBe agent.controllers.routes.TermsController.show().url
    }
  }

  authorisationTests()

}
