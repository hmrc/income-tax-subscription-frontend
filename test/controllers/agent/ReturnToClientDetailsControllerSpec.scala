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

import controllers.agent.matching.ReturnToClientDetailsController
import forms.agent.ReturnToClientDetailsForm
import models.ReturnToClientDetailsModel.{ContinueWithCurrentClient, SignUpAnotherClient}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.agent.mocks.MockReturnToClientDetails


class ReturnToClientDetailsControllerSpec extends AgentControllerBaseSpec
  with MockReturnToClientDetails
  with MockSubscriptionDetailsService
  with MockAuditingService {

  override val controllerName: String = "ReturnToClientDetails"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestReturnToClientDetails.show(),
    "submit" -> TestReturnToClientDetails.submit()
  )

  object TestReturnToClientDetails extends ReturnToClientDetailsController(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockReturnToClientDetails
  )

  "show" should {
    "display the return to client details page and return OK (200)" in {
      mockView()
      val result = TestReturnToClientDetails.show()(subscriptionRequestWithName)
      status(result) must be(Status.OK)
      contentType(result) mustBe Some(HTML)
    }
  }
  "submit" when {
    "Continue with current client is selected" should {
      "Redirect to the home controller" when {
        "the user is eligible for the current tax year based on session flags" in {
          val result = TestReturnToClientDetails.submit()(
            subscriptionRequestWithName.post(ReturnToClientDetailsForm.returnToClientDetailsForm, ContinueWithCurrentClient)
          )
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.agent.matching.routes.HomeController.home.url)
        }
      }
      "Redirect to the Cannot Sign Up This Year page" when {
        "the user is only eligible for the next tax year based on session flags" in {
          val result = TestReturnToClientDetails.submit()(
            subscriptionRequestWithNameNextYearOnly.post(ReturnToClientDetailsForm.returnToClientDetailsForm, ContinueWithCurrentClient)
          )
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
        }
      }
    }
    "Sign Up Another Client is selected" should {
      "Redirect to Enter Client Details page" in {
        val result = TestReturnToClientDetails.submit()(
          subscriptionRequestWithName.post(ReturnToClientDetailsForm.returnToClientDetailsForm, SignUpAnotherClient)
        )
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.AddAnotherClientController.addAnother().url)
      }
    }
    "No Option is selected" should {
      "return an Bad Request" in {
        mockView()
        val result = TestReturnToClientDetails.submit()(subscriptionRequestWithName.post(ReturnToClientDetailsForm.returnToClientDetailsForm))
        status(result) must be(Status.BAD_REQUEST)
        contentType(result) mustBe Some(HTML)
      }
    }
  }
}
