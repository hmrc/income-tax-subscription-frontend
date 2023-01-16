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

package controllers.agent.eligibility

import controllers.agent.AgentControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.agent.eligibility.CannotSignUpThisYear

class CannotSignUpThisYearControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService  {
  override val controllerName: String = "CannotSignUpController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private val view = mock[CannotSignUpThisYear]

  object TestCannotSignUpThisYearController extends CannotSignUpThisYearController(
    mockAuditingService,
    mockAuthService,
    view
  )

  "show" should {
    "display the property accounting method view and return OK (200)" in {
      when(view(any(), any())(any(), any()))
        .thenReturn(HtmlFormat.empty)

      val result: Result = await(TestCannotSignUpThisYearController.show()(subscriptionRequest))

      status(result) must be(Status.OK)
    }
  }

  "submit" should {
    "redirect to the home controller" in {
      val result: Result = await(TestCannotSignUpThisYearController.submit()(subscriptionRequest))

      status(result) must be(Status.SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.agent.routes.HomeController.home.url))
    }
  }

}
