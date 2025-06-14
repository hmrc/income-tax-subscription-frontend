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

package controllers.agent.matching

import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import messagelookup.agent.MessageLookup.{ClientDetailsLockout => messages}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Cookie, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockUserLockoutService
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.matching.ClientDetailsLockout

import java.time.Duration

class ClientDetailsLockoutControllerSpec extends AgentControllerBaseSpec
  with MockUserLockoutService with MockIdentifierAction{

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "ClientDetailsLockoutController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: ClientDetailsLockoutController => Any): Unit = {

    val clientDetailsLockoutView = app.injector.instanceOf[ClientDetailsLockout]
    val controller = new ClientDetailsLockoutController(
      fakeIdentifierAction,
      mockUserLockoutService,
      clientDetailsLockoutView
    )
    testCode(controller)
  }

  "Calling the 'show' action of the ClientDetailsLockoutController" when {

    "the agent is locked out" should withController { controller =>
      lazy val result = controller.show(userMatchingRequest)
      lazy val document = Jsoup.parse(contentAsString(result))

      "return 200" in {
        setupMockLockedOut(testARN)
        status(result) must be(Status.OK)
      }

      "return HTML" in {
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
      }

      "render the 'Client Details Lockout page'" in {
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        document.title mustBe messages.title + serviceNameGovUk
      }
    }

    "the agent is not locked out" should {
      s"redirect to ${controllers.agent.matching.routes.ClientDetailsController.show().url}" in withController { controller =>
        setupMockNotLockedOut(testARN)

        lazy val result = controller.show(userMatchingRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url
      }
    }

    "there is a failure response from the lockout service" should {
      "return an internal server exception" in withController { controller =>
        setupMockLockStatusFailureResponse(testARN)

        lazy val result = controller.show(userMatchingRequest)

        intercept[InternalServerException](await(result)).getMessage mustBe "[ClientDetailsLockoutController][handleLockOut] lockout status failure"
      }
    }

  }

  "durationText" when {
    "the language is English" should {
      implicit lazy val r: Request[_] = FakeRequest()
      "convert time using correct singular units" in withController { controller =>
        val testDuration = List(Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1)).reduce(_.plus(_))
        controller.durationText(testDuration) mustBe "1 hour 1 minute 1 second"
      }

      "convert time using correct plural units" in withController { controller =>
        val testDuration = List(Duration.ofHours(2), Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration) mustBe "2 hours 2 minutes 2 seconds"
      }

      "convert different combinations of hour minute seconds correctly" in withController { controller =>
        val testDuration1 = List(Duration.ofHours(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration1) mustBe "2 hours 2 seconds"
        val testDuration2 = List(Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration2) mustBe "2 minutes 2 seconds"
        val testDuration3 = List(Duration.ofMinutes(2)).reduce(_.plus(_))
        controller.durationText(testDuration3) mustBe "2 minutes"
      }
    }

    "the language is Welsh" should {
      implicit lazy val r: Request[_] = FakeRequest().withCookies(Cookie(messagesApi.langCookieName, "cy"))
      "convert time using correct singular units" in withController { controller =>
        val testDuration = List(Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1)).reduce(_.plus(_))
        controller.durationText(testDuration) mustBe "1 awr 1 munud 1 eiliad"
      }

      "convert time using correct plural units" in withController { controller =>
        val testDuration = List(Duration.ofHours(2), Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration) mustBe "2 oriau 2 munudau 2 eiliadau"
      }

      "convert different combinations of hour minute seconds correctly" in withController { controller =>
        val testDuration1 = List(Duration.ofHours(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration1) mustBe "2 oriau 2 eiliadau"
        val testDuration2 = List(Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
        controller.durationText(testDuration2) mustBe "2 munudau 2 eiliadau"
        val testDuration3 = List(Duration.ofMinutes(2)).reduce(_.plus(_))
        controller.durationText(testDuration3) mustBe "2 munudau"
      }
    }
  }

  authorisationTests()

}
