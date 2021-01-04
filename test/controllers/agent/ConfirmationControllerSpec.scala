/*
 * Copyright 2021 HM Revenue & Customs
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

import models.usermatching.UserDetailsModel
import org.jsoup.Jsoup
import org.scalatest.Matchers._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.{MockAccountingPeriodService, MockSubscriptionDetailsService, MockUserMatchingService}
import uk.gov.hmrc.http.NotFoundException
import utilities.TestModels
import utilities.agent.TestModels._
import uk.gov.hmrc.http.InternalServerException
import utilities.agent.TestConstants.testException



class ConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAccountingPeriodService with MockUserMatchingService {

  object TestConfirmationController extends ConfirmationController(
    mockAuthService,
    mockAccountingPeriodService,
    MockSubscriptionDetailsService
  )(executionContext, appConfig, mockMessagesControllerComponents)

  val userDetails: UserDetailsModel = TestModels.testUserDetails

  val taxQuarter1 = ("agent.sign-up.complete.julyUpdate", "2020")
  val taxQuarter2 = ("agent.sign-up.complete.octoberUpdate", "2020")
  val taxQuarter3 = ("agent.sign-up.complete.januaryUpdate", "2021")
  val taxQuarter4 = ("agent.sign-up.complete.aprilUpdate", "2021")

  implicit val request: Request[_] = FakeRequest()

  override val controllerName: String = "ConfirmationControllerSpec"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showConfirmation" -> TestConfirmationController.show
  )

  "ConfirmationController" when {

    "submitted is not in session" should {
      "return a NotFoundException" in {
        val result = TestConfirmationController.show(subscriptionRequest)

        intercept[NotFoundException](await(result))
      }
    }

    "submitted is in session" should {
      "return OK" in {
        mockFetchAllFromSubscriptionDetails(testCacheMap)

        mockUpdateDateBefore(List(taxQuarter1, taxQuarter2))
        mockUpdateDateAfter(List(taxQuarter3, taxQuarter4))

        val result = TestConfirmationController.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any").buildRequest(userDetails))
        status(result) shouldBe OK
      }
    }

    "no client details in session" should {
      "return an exception" in {
        mockFetchAllFromSubscriptionDetails(testCacheMap)

        mockUpdateDateBefore(List(taxQuarter1, taxQuarter2))
        mockUpdateDateAfter(List(taxQuarter3, taxQuarter4))

        val exception = intercept[Exception](await(TestConfirmationController.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any"))))
        exception.getMessage shouldBe "[ConfirmationController][show]-could not retrieve client name from session"
      }
    }

    "submitted is in session and new Confirmation content applies" should {
      "return OK" in {
        mockFetchAllFromSubscriptionDetails(testCacheMap)

        mockUpdateDateBefore(List(taxQuarter1, taxQuarter2))
        mockUpdateDateAfter(List(taxQuarter3, taxQuarter4))

        val clientName = userDetails.firstName + " " + userDetails.lastName

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
            .buildRequest(userDetails)
        )
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("agent.sign-up-complete.title", clientName) + serviceNameGovUk
      }
    }

  }

  authorisationTests()

}
