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

import agent.assets.MessageLookup.{ClientAlreadySubscribed => messages}
import agent.audit.mocks.MockAuditingService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class ClientAlreadySubscribedControllerSpec extends AgentControllerBaseSpec with MockAuditingService {

  override val controllerName: String = "ClientAlreadySubscribedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestClientAlreadySubscribedController.show(),
    "submit" -> TestClientAlreadySubscribedController.submit()
  )

  object TestClientAlreadySubscribedController extends ClientAlreadySubscribedController(
    mockAuditingService,
    mockAuthService
  )(executionContext, appConfig, mockMessagesControllerComponents)

  "Calling the show action of the ClientAlreadySubscribedController with an Authenticated User" should {

    lazy val result = TestClientAlreadySubscribedController.show(userMatchingRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"render the already subscribed page" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document.title mustBe messages.heading + serviceNameGovUk
    }

    s"the post action of the page rendered should be '${controllers.agent.routes.ClientAlreadySubscribedController.submit().url}'" in {
      document.select("form").attr("action") mustBe controllers.agent.routes.ClientAlreadySubscribedController.submit().url
    }

  }

  "Calling the submit action of the ClientAlreadySubscribedController with an Authenticated User" should {

    lazy val result = TestClientAlreadySubscribedController.submit(userMatchingRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    s"redirect to '${controllers.agent.matching.routes.ClientDetailsController.show().url}'" in {
      redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
    }

  }

  authorisationTests()
}
