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

package controllers.agent

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec}
import play.api.test.Helpers.{HTML, charset, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockAuditingService
import views.html.agent.throttling.{ThrottleEndOfJourney, ThrottleStartOfJourney}


class ThrottlingControllerSpec extends AgentControllerBaseSpec with MockAuditingService {
  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "start" should {
    "return the throttle start Of journey page" in withController { controller =>
      val result = controller.start()(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }

  "end" should {
    "return the throttle end Of journey page" in withController { controller =>
      val result = controller.end()(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }


  private def withController(testCode: ThrottlingController => Any): Unit = {
    val startOfJourneyView = mock[ThrottleStartOfJourney]

    when(startOfJourneyView(
      ArgumentMatchers.eq(controllers.agent.matching.routes.ConfirmedClientResolver.resolve)
    )(any(), any())).thenReturn(HtmlFormat.empty)

    val endOfJourneyView = mock[ThrottleEndOfJourney]

    when(endOfJourneyView(
      ArgumentMatchers.eq(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show)
    )(any(), any())).thenReturn(HtmlFormat.empty)

    val controller = new ThrottlingController(
      mockAuditingService,
      mockAuthService,
      startOfJourneyView,
      endOfJourneyView
    )

    testCode(controller)
  }
}
