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

package controllers.agent.matching

import config.MockConfig
import controllers.ControllerSpec
import controllers.agent.actions.mocks.MockIdentifierAction
import forms.agent.CannotGoBackToPreviousClientForm
import forms.agent.CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClientForm
import models.CannotGoBack.{AgentServiceAccount, ReenterClientDetails, SignUpAnotherClient}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import views.agent.mocks.MockCannotGoBackToPreviousClient

import scala.concurrent.Future


class CannotGoBackToPreviousClientControllerSpec extends ControllerSpec
  with MockCannotGoBackToPreviousClient
  with MockIdentifierAction {

  object TestCannotGoBackToPreviousClient extends CannotGoBackToPreviousClientController(
    mockCannotGoBackToPreviousClient,
    fakeIdentifierAction,
    MockConfig
  )

  "show" must {
    "return OK with the page content" in {
      mockView(
        cannotGoBackToPreviousClientForm = cannotGoBackToPreviousClientForm,
        postAction = routes.CannotGoBackToPreviousClientController.submit
      )

      val result: Future[Result] = TestCannotGoBackToPreviousClient.show()(request)

      status(result) must be(Status.OK)
      contentType(result) mustBe Some(HTML)
    }
  }
  "submit" must {
    "redirect to the agent services account" when {
      "agent services account is selected" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          request
            .withMethod("POST")
            .withFormUrlEncodedBody(CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient -> AgentServiceAccount.key)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(MockConfig.agentServicesAccountHomeUrl)
      }
    }
    "redirect to the add another client route" when {
      "re-enter client details is selected" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          request
            .withMethod("POST")
            .withFormUrlEncodedBody(CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient -> ReenterClientDetails.key)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.AddAnotherClientController.addAnother().url)
      }
      "sign up another client is selected" in {
        val result = TestCannotGoBackToPreviousClient.submit()(
          request
            .withMethod("POST")
            .withFormUrlEncodedBody(CannotGoBackToPreviousClientForm.cannotGoBackToPreviousClient -> SignUpAnotherClient.key)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.AddAnotherClientController.addAnother().url)
      }
    }
    "return a bad request" when {
      "no option was selected" in {
        mockView(
          cannotGoBackToPreviousClientForm = cannotGoBackToPreviousClientForm.bind(Map.empty[String, String]),
          postAction = routes.CannotGoBackToPreviousClientController.submit
        )

        val result: Future[Result] = TestCannotGoBackToPreviousClient.submit()(
          request.withMethod("POST").withFormUrlEncodedBody()
        )

        status(result) mustBe BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }
}
