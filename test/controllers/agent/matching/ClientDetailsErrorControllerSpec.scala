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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers.{contentAsString, contentType, _}
import play.twirl.api.HtmlFormat
import views.html.agent.ClientDetailsError

import scala.concurrent.Future

class ClientDetailsErrorControllerSpec extends AgentControllerBaseSpec with MockAuditingService {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "ClientDetailsErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val mockClientDetailsError: ClientDetailsError = mock[ClientDetailsError]
  when(mockClientDetailsError(ArgumentMatchers.any())(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestClientDetailsErrorController extends ClientDetailsErrorController(
    mockAuditingService,
    mockAuthService,
    mockClientDetailsError
  )

  "Calling the 'show' action of the ClientDetailsErrorController" should {

    lazy val result = TestClientDetailsErrorController.show(userMatchingRequest)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
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
