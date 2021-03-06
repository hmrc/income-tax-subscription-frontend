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

import agent.audit.mocks.MockAuditingService
import forms.agent.ClientDetailsForm.clientNino
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

import scala.util.matching.Regex


class ConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAccountingPeriodService
  with MockUserMatchingService with MockAuditingService {

  object TestConfirmationController extends ConfirmationController(
    mockAuditingService,
    mockAuthService,
    mockAccountingPeriodService,
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
    "showConfirmation" -> TestConfirmationController.show
  )

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }

  }

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

        val clientNino = formatNino(userDetails.nino)

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
            .buildRequest(userDetails)
        )
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("agent.sign-up-complete.title", clientName, clientNino) + serviceNameGovUk
      }
    }

  }

  authorisationTests()

}
