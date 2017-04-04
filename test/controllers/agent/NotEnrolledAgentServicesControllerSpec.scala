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

package controllers.agent

import assets.MessageLookup.AgentMessages.{NotEnrolledAgentServices => messages}
import auth._
import controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class NotEnrolledAgentServicesControllerSpec extends ControllerBaseSpec {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "NotEnrolledAgentServices"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestNotEnrolledAgentServicesController extends NotEnrolledAgentServicesController(
    MockBaseControllerConfig,
    messagesApi
  )

  "Calling the 'show' action of the NotEnrolledAgentServicesController" should {

    lazy val result = TestNotEnrolledAgentServicesController.show(authenticatedFakeRequest())
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the 'Not subscribed to Agent Services page'" in {
      document.title mustBe messages.heading
    }

    s"the page must have a link to ${controllers.routes.SignOutController.signOut().url}" in {
      document.select("#sign-out").attr("href") mustBe controllers.routes.SignOutController.signOut().url
    }

  }
}
