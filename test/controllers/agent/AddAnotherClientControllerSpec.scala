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

package controllers.agent

import common.Constants.ITSASessionKeys
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockSessionDataService, MockSubscriptionDetailsService, MockUserLockoutService}
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil

import scala.concurrent.Future


class AddAnotherClientControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockUserLockoutService
  with MockSessionDataService
  with MockAuditingService {

  override val controllerName: String = "addAnotherClientController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "addAnother" -> TestAddAnotherClientController.addAnother()
  )

  object TestAddAnotherClientController extends AddAnotherClientController(
    mockAuditingService,
    mockAuthService,
    mockSessionDataService,
    appConfig
  )(executionContext, mockMessagesControllerComponents)

  val fullSessionDetailsRequest = FakeRequest().withSession(
    ITSASessionKeys.JourneyStateKey -> "test-journey-state",
    ITSASessionKeys.MTDITID -> "test-mtditid",
    ITSASessionKeys.NINO -> "test-nino",
    ITSASessionKeys.UTR -> "test-utr",
    ITSASessionKeys.ELIGIBLE_NEXT_YEAR_ONLY -> "test-eligible-next-year-only",
    ITSASessionKeys.MANDATED_CURRENT_YEAR -> "test-mandated-current-year",
    ITSASessionKeys.MANDATED_NEXT_YEAR -> "test-mandated-next-year",
    UserMatchingSessionUtil.firstName -> "test-first-name",
    UserMatchingSessionUtil.lastName -> "test-last-name"
  )

  "AddAnotherClientController.addAnother" should {

    def call: Future[Result] = TestAddAnotherClientController.addAnother()(fullSessionDetailsRequest)

    "redirect to the agent eligibility frontend terms page, clearing Subscription Details and session values" in {
      mockDeleteReferenceSuccess()

      val result: Result = await(call)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      session(result).data mustBe Map.empty[String, String]
      result.verifyStoredUserDetailsIs(None)(fullSessionDetailsRequest)
    }

    "throw an InternalServerException if the delete reference call failed" in {
      mockDeleteReferenceStatusFailure(INTERNAL_SERVER_ERROR)

      intercept[InternalServerException](await(call))
        .message mustBe s"[AddAnotherClientController][addAnother] - Unexpected status deleting reference from session. Status: $INTERNAL_SERVER_ERROR"
    }
  }

  authorisationTests()

}
