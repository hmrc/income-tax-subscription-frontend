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

package controllers.usermatching

import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.agent.AgentAffinityGroupError

class AffinityGroupErrorControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "AffinityGroupErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private val agentAffinityGroupError = mock[AgentAffinityGroupError]

  when(agentAffinityGroupError()(any(), any(), any()))
    .thenReturn(HtmlFormat.empty)

  object TestAffinityGroupErrorController extends AffinityGroupErrorController(agentAffinityGroupError, mockMessagesControllerComponents)

  "Calling the show action of the AffinityGroupErrorController" should {

    lazy val result = TestAffinityGroupErrorController.show(subscriptionRequest)

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }
  }
}
