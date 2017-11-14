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
import agent.services.mocks.MockKeystoreService
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, NotFoundException}


class AddAnotherClientControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "addAnotherClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "addAnother" -> TestAddAnotherClientController.addAnother()
  )

  object TestAddAnotherClientController extends AddAnotherClientController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    app.injector.instanceOf[Logging]
  )

  "AddAnotherClientController.addAnother" when {

    "the user has not completed their submission" should {
      setupMockKeystore(fetchSubscriptionId = "testId")

      def call = TestAddAnotherClientController.addAnother()(subscriptionRequest)

      "return a NotFoundException" in {
        intercept[NotFoundException](await(call))
      }
    }

    "the user has completed their submission" should {
      lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "anyValue")

      def call = TestAddAnotherClientController.addAnother()(request)

      s"redirect to ${agent.controllers.matching.routes.ClientDetailsController.show().url}" in {
        setupMockKeystore(deleteAll = HttpResponse(OK))

        val result = call

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(agent.controllers.matching.routes.ClientDetailsController.show().url)
      }

      "cleared the keystore" in {
        setupMockKeystore(deleteAll = HttpResponse(OK))

        val result = call

        await(result)
        verifyKeystore(deleteAll = 1)
      }

      s"removed the ${ITSASessionKeys.MTDITID} from session" in {
        setupMockKeystore(deleteAll = HttpResponse(OK))

        // test validity check
        request.session.get(ITSASessionKeys.MTDITID) must not be None
        request.session.get(ITSASessionKeys.JourneyStateKey) must not be None
        request.session.get(ITSASessionKeys.UTR) must not be None
        request.session.get(ITSASessionKeys.NINO) must not be None

        val result = await(call)

        result.session(request).get(ITSASessionKeys.MTDITID) mustBe None
        result.session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
        result.session(request).get(ITSASessionKeys.UTR) mustBe None
        result.session(request).get(ITSASessionKeys.NINO) mustBe None
      }
    }
  }

  authorisationTests()

}
