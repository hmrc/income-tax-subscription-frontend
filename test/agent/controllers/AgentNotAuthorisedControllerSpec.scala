/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import agent.assets.MessageLookup.{AgentNotAuthorisedError => messages}
import core.config.MockConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class AgentNotAuthorisedControllerSpec extends AgentControllerBaseSpec {

  override val controllerName: String = "AgentNotAuthorisedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestAgentNotAuthorisedController.show(),
    "submit" -> TestAgentNotAuthorisedController.submit()
  )

  private def createTestAgentNotAuthorisedController(enableMatchingFeature: Boolean) = new AgentNotAuthorisedController(
    mockBaseControllerConfig(new MockConfig {
      override val unauthorisedAgentEnabled = enableMatchingFeature
    }),
    messagesApi,
    mockAuthService
  )


  lazy val TestAgentNotAuthorisedController = createTestAgentNotAuthorisedController(enableMatchingFeature = true)

  "Calling the show action of the AgentNotAuthorisedController with an Authenticated User" should {

    lazy val result = TestAgentNotAuthorisedController.show(userMatchedRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"render the agent not authorised page" in {
      document.title mustBe messages.heading
    }

    s"the post action of the page rendered should be '${agent.controllers.routes.AgentNotAuthorisedController.submit().url}'" in {
      document.select("form").attr("action") mustBe agent.controllers.routes.AgentNotAuthorisedController.submit().url
    }

  }

  "Calling the submit action of the AgentNotAuthorisedController with an Authenticated User" should {

    lazy val result = TestAgentNotAuthorisedController.submit(userMatchedRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    s"redirect to '${agent.controllers.routes.HomeController.index().url}'" in {
      redirectLocation(result) mustBe Some(agent.controllers.routes.HomeController.index().url)
    }

  }

  authorisationTests()
}
