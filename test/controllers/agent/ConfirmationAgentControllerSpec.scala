/*
 * Copyright 2022 HM Revenue & Customs
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
import models.usermatching.UserDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks._
import utilities.TestModels
import utilities.agent.TestModels._
import views.html.agent.SignUpComplete
import views.html.agent.SignUpConfirmation


class ConfirmationAgentControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockUserMatchingService
  with MockAuditingService
  with MockSpsService {

  val mockSignUpComplete: SignUpComplete = mock[SignUpComplete]
  val mockSignUpConfirmation: SignUpConfirmation = mock[SignUpConfirmation]


  object TestConfirmationAgentController$ extends ConfirmationAgentController(
    mockAuditingService,
    mockAuthService,
    mockSignUpComplete,
    mockSignUpConfirmation,
    MockSubscriptionDetailsService
  )(executionContext, appConfig, mockMessagesControllerComponents)

  val userDetails: UserDetailsModel = TestModels.testUserDetails

  val taxQuarter1: (String, String) = ("agent.sign-up.complete.julyUpdate", "2020")
  val taxQuarter2: (String, String) = ("agent.sign-up.complete.octoberUpdate", "2020")
  val taxQuarter3: (String, String) = ("agent.sign-up.complete.januaryUpdate", "2021")
  val taxQuarter4: (String, String) = ("agent.sign-up.complete.aprilUpdate", "2021")

  implicit val request: Request[_] = FakeRequest()

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationAgentController$.show
  )

  private def mockCall() =
    when(mockSignUpComplete(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(HtmlFormat.empty)


  "ConfirmationController" when {

    "submitted is not in session" should {
      "return a NotFoundException" in {
        TestConfirmationAgentController$.show(subscriptionRequest)
      }
    }

    "submitted is in session" should {
      "return OK" in {
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))

        mockCall()
        val result = TestConfirmationAgentController$.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any").buildRequest(Some(userDetails)))
        status(result) mustBe OK
      }
    }

    "no client details in session" should {
      "return an exception" in {
        mockCall()


        val exception = intercept[Exception](await(TestConfirmationAgentController$.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any"))))
        exception.getMessage mustBe "[ConfirmationController][show]-could not retrieve client name from session"
      }
    }

    "submitted is in session and new Confirmation content applies" should {
      "return OK" in {
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearNext))
        
        mockCall()

        val result = TestConfirmationAgentController$.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
            .buildRequest(Some(userDetails))
        )

        status(result) mustBe OK
      }
    }

  }
  authorisationTests()

}
