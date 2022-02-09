/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentType, _}
import play.twirl.api.HtmlFormat
import views.html.agent.NotEnrolledAgentServices

class NotEnrolledAgentServicesControllerSpec extends AgentControllerBaseSpec {

  // Required for trait but no authorisation tests are required
  override val controllerName: String = "NotEnrolledAgentServices"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val mockNotEnrolledAgentService: NotEnrolledAgentServices = mock[NotEnrolledAgentServices]
  when(mockNotEnrolledAgentService()(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestNotEnrolledAgentServicesController extends NotEnrolledAgentServicesController(
    mockNotEnrolledAgentService,
    mockMessagesControllerComponents)

  "Calling the 'show' action of the NotEnrolledAgentServicesController" should {

    lazy val result = TestNotEnrolledAgentServicesController.show(subscriptionRequest)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

  }
}
