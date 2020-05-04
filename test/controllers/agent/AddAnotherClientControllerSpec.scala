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

import config.featureswitch.FeatureSwitching
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockKeystoreService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future


class AddAnotherClientControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with FeatureSwitching {

  override val controllerName: String = "addAnotherClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "addAnother" -> TestAddAnotherClientController.addAnother()
  )

  object TestAddAnotherClientController extends AddAnotherClientController(
    mockAuthService,
    appConfig,
    MockKeystoreService
  )(executionContext, mockMessagesControllerComponents)

  "AddAnotherClientController.addAnother" should {

    lazy val request = subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "anyValue")

    def call: Future[Result] = TestAddAnotherClientController.addAnother()(request)


    "redirect to the agent eligibility frontend terms page, clearing keystore and session values" in {
      mockDeleteAllFromKeyStore(HttpResponse(OK))

      val result: Result = await(call)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxEligibilityFrontendUrl}/client/other-income")

      result.session(request).get(ITSASessionKeys.MTDITID) mustBe None
      result.session(request).get(ITSASessionKeys.JourneyStateKey) mustBe None
      result.session(request).get(ITSASessionKeys.UTR) mustBe None
      result.session(request).get(ITSASessionKeys.NINO) mustBe None

      verifyKeystore(deleteAll = 1)
    }


  }

  authorisationTests()

}
