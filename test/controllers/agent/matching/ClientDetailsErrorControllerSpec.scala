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

package controllers.agent.matching

import agent.assets.MessageLookup.{ClientDetailsError => messages}
import agent.audit.mocks.MockAuditingService
import controllers.agent.AgentControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, contentType, _}

class ClientDetailsErrorControllerSpec extends AgentControllerBaseSpec with MockAuditingService {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "ClientDetailsErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestClientDetailsErrorController.show,
    "submit" -> TestClientDetailsErrorController.submit
  )

  object TestClientDetailsErrorController extends ClientDetailsErrorController(
    mockAuditingService,
    mockAuthService
  )

  "Calling the 'show' action of the ClientDetailsErrorController" should {

    lazy val result = TestClientDetailsErrorController.show(userMatchingRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "render the 'Client Details Error page'" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document.title mustBe messages.title + serviceNameGovUk
    }

    s"the page must have a link to sign out}" in {
      document.select("#sign-out").attr("href") mustBe
        controllers.SignOutController.signOut(userMatchingRequest.path).url
    }

  }

  "Calling the 'submit' action of the ClientDetailsErrorController" should {

    lazy val result = TestClientDetailsErrorController.submit(userMatchingRequest)

    "return 303" in {
      status(result) must be(Status.SEE_OTHER)
    }

    "Redirect to the 'Client details' page" in {
      redirectLocation(result).get mustBe controllers.agent.matching.routes.ClientDetailsController.show().url
    }

  }

  authorisationTests()

}
