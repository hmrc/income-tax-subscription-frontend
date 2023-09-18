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

import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.agent.YourIncomeSourceToSignUp

class YourIncomeSourceToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockAuditingService {
  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()


  "show" should {
    "return OK status" in {

      withController { controller =>
        val result = await(controller.show()(subscriptionRequestWithName))
        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }

  }


  private def withController(testCode: YourIncomeSourceToSignUpController => Assertion) = {
    val yourIncomeSourceToSignUpView = mock[YourIncomeSourceToSignUp]

    when(yourIncomeSourceToSignUpView(
      any(),
      any()
    )(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new YourIncomeSourceToSignUpController(
      yourIncomeSourceToSignUpView,
      MockSubscriptionDetailsService,
      mockAuditingService,
      mockAuthService
    )

    testCode(controller)
  }

}
