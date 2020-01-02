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
import agent.services.mocks.MockKeystoreService
import core.config.featureswitch.{EligibilityPagesFeature, FeatureSwitching}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse


class AddAnotherClientControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(EligibilityPagesFeature)
  }

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

    lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "anyValue", ITSASessionKeys.UnauthorisedAgentKey -> "anyValue")

    def call = TestAddAnotherClientController.addAnother()(request)

    "eligibility feature switch is enabled" should {

      "redirect to the agent eligibility frontend terms page, clearing keystore and session values" in {
        enable(EligibilityPagesFeature)

        setupMockKeystore(deleteAll = HttpResponse(OK))

        val result: Result = await(call)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(s"${MockBaseControllerConfig.applicationConfig.incomeTaxEligibilityFrontendUrl}/client/other-income")

        result.session(request).get(ITSASessionKeys.MTDITID) mustBe None
        result.session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
        result.session(request).get(ITSASessionKeys.UnauthorisedAgentKey) mustBe None
        result.session(request).get(ITSASessionKeys.UTR) mustBe None
        result.session(request).get(ITSASessionKeys.NINO) mustBe None

        verifyKeystore(deleteAll = 1)
      }

    }

    "eligibility feature switch is disabled" should {

      s"redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in {
        setupMockKeystore(deleteAll = HttpResponse(OK))

        val result: Result = await(call)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)

        result.session(request).get(ITSASessionKeys.MTDITID) mustBe None
        result.session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
        result.session(request).get(ITSASessionKeys.UnauthorisedAgentKey) mustBe None
        result.session(request).get(ITSASessionKeys.UTR) mustBe None
        result.session(request).get(ITSASessionKeys.NINO) mustBe None

        verifyKeystore(deleteAll = 1)
      }

    }

  }

  authorisationTests()

}
