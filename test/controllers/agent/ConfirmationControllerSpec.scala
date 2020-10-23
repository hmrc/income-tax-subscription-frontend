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

import org.jsoup.Jsoup
import org.scalatest.Matchers._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.NotFoundException
import utilities.agent.TestModels._

class ConfirmationControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService {

  object TestConfirmationController extends ConfirmationController(
    mockAuthService,
    MockSubscriptionDetailsService
  )(executionContext, appConfig, mockMessagesControllerComponents)

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

        val result = TestConfirmationController.show(subscriptionRequest.addingToSession(ITSASessionKeys.MTDITID -> "any"))
        status(result) shouldBe OK
      }
    }

    "submitted is in session and new Confirmation content applies" should {
      "return OK" in {
        mockFetchAllFromSubscriptionDetails(testCacheMap)

        val result = TestConfirmationController.show(
          subscriptionRequest
            .addingToSession(ITSASessionKeys.MTDITID -> "any")
        )
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        status(result) shouldBe OK

        Jsoup.parse(contentAsString(result)).title shouldBe Messages("agent.sign-up-complete.title") + serviceNameGovUk
      }
    }

  }

  authorisationTests()

}
