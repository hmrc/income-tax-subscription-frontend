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

package controllers

import assets.MessageLookup.{AlreadyEnrolled => messages}
import auth._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds

class AlreadyEnrolledControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "NotEnrolledAgentServicesController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "enrolled" -> TestAlreadyEnrolledController.enrolled()
  )

  object TestAlreadyEnrolledController extends AlreadyEnrolledController(
    MockBaseControllerConfig,
    messagesApi
  )

  "Calling the enrolled action of the NotEnrolledAgentServicesController with a not enrolled Authenticated User" should {

    lazy val result = TestAlreadyEnrolledController.enrolled(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 404" in {
      status(result) must be(Status.NOT_FOUND)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the not found error page" in {
      document.title mustBe "Page not found - 404"
      document.getElementsByTag("h1").text() mustBe "This page can’t be found"
    }

  }

  "Calling the enrolled action of the NotEnrolledAgentServicesController with an enrolled Authenticated User" should {

    lazy val result = TestAlreadyEnrolledController.enrolled(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockEnrolled))
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"render the already enrolled page" in {
      document.title mustBe messages.heading
    }

    s"the post action of the page rendered should be '${controllers.routes.SignOutController.signOut().url}'" in {
      document.select("form").attr("action") mustBe controllers.routes.SignOutController.signOut().url
    }

  }

  authorisationTests()
}
