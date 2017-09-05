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

package controllers.matching

import java.time.Duration

import assets.MessageLookup.{UserDetailsLockout => messages}
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, contentType, _}
import services.mocks.MockUserLockoutService
import utils.TestConstants._

class UserDetailsLockoutControllerSpec extends ControllerBaseSpec
  with MockUserLockoutService {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "UserDetailsLockoutController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestUserDetailsLockoutController.show,
    "submit" -> TestUserDetailsLockoutController.submit
  )

  object TestUserDetailsLockoutController extends UserDetailsLockoutController(
    MockBaseControllerConfig,
    messagesApi,
    mockAuthService,
    mockUserLockoutService
  )

  "Calling the 'show' action of the ClientDetailsLockoutController" when {

    "the agent is locked out" should {
      lazy val result = TestUserDetailsLockoutController.show(fakeRequest)
      lazy val document = Jsoup.parse(contentAsString(result))

      "return 200" in {
        setupMockLockedOut(testNino)
        status(result) must be(Status.OK)
      }

      "return HTML" in {
        contentType(result) must be(Some("text/html"))
        charset(result) must be(Some("utf-8"))
      }

      "render the 'Client Details Lockout page'" in {
        document.title mustBe messages.title
      }
    }

    "the agent is not locked out" should {
      s"redirect to ${controllers.matching.routes.UserDetailsController.show().url}" in {
        setupMockNotLockedOut(testNino)

        lazy val result = TestUserDetailsLockoutController.show(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.matching.routes.UserDetailsController.show().url
      }
    }

  }

  "Calling the 'submit' action of the ClientDetailsLockoutController" should {

    lazy val result = TestUserDetailsLockoutController.submit(fakeRequest)

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    "Redirect to the 'Client details' page" in {
      redirectLocation(result).get mustBe controllers.routes.SignOutController.signOut().url
    }

  }

  "durationText" should {
    "convert time using correct singular units" in {
      val testDuration = List(Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(1)).reduce(_.plus(_))
      TestUserDetailsLockoutController.durationText(testDuration) mustBe "1 hour 1 minute 1 second"
    }

    "convert time using correct plural units" in {
      val testDuration = List(Duration.ofHours(2), Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
      TestUserDetailsLockoutController.durationText(testDuration) mustBe "2 hours 2 minutes 2 seconds"
    }

    "convert different combinations of hour minute seconds correctly" in {
      val testDuration1 = List(Duration.ofHours(2), Duration.ofSeconds(2)).reduce(_.plus(_))
      TestUserDetailsLockoutController.durationText(testDuration1) mustBe "2 hours 2 seconds"
      val testDuration2 = List(Duration.ofMinutes(2), Duration.ofSeconds(2)).reduce(_.plus(_))
      TestUserDetailsLockoutController.durationText(testDuration2) mustBe "2 minutes 2 seconds"
      val testDuration3 = List(Duration.ofMinutes(2)).reduce(_.plus(_))
      TestUserDetailsLockoutController.durationText(testDuration3) mustBe "2 minutes"
    }
  }

  authorisationTests()

}
