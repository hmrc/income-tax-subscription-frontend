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

import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.agent.mocks.MockCannotGoBackToPreviousClient
import forms.agent.CannotGoBackToPreviousClientForm
import models.CannotGoBack.{AgentServiceAccount, ReenterClientDetails, SignUpAnotherClient}


class CannotGoBackToPreviousClientControllerSpec extends AgentControllerBaseSpec
  with MockCannotGoBackToPreviousClient
  with MockSubscriptionDetailsService
  with MockAuditingService {

  override val controllerName: String = "CannotGoBackToPreviousClient"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCannotGoBackToPreviousClient.show(),
    "submit" -> TestCannotGoBackToPreviousClient.submit()
  )

  object TestCannotGoBackToPreviousClient extends CannotGoBackToPreviousClientController(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockCannotGoBackToPreviousClient
  )

  "show" should {
    "display the Cannot go back to previous client page and return OK (200)" in {
      mockView()
      val result = TestCannotGoBackToPreviousClient.show()(fakeRequest)
      status(result) must be(Status.OK)
      contentType(result) mustBe Some(HTML)
    }
  }
  "submit" when {
    "Agent services account is selected" should {
      "Redirect to the Agent Services Account page" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          fakeRequest.post(CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm, AgentServiceAccount)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(appConfig.agentServicesAccountHomeUrl)
      }
    }
    "Re-enter Client Details is selected" should {
      "Redirect to Enter Client Details page" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          fakeRequest.post(CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm, ReenterClientDetails)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }
    "Sign Up Another Client is selected" should {
      "Redirect to Enter Client Details page" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          fakeRequest.post(CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm, SignUpAnotherClient)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ClientDetailsController.show().url)
      }
    }
    "No Option is selected" should {
      "return an Bad Request" in {
        mockView()
        val result = TestCannotGoBackToPreviousClient.submit()(fakeRequest.post(CannotGoBackToPreviousClientForm.cannotGoBackTpPreviousClientForm))
        status(result) must be(Status.BAD_REQUEST)
        contentType(result) mustBe Some(HTML)
      }
    }
  }
}
