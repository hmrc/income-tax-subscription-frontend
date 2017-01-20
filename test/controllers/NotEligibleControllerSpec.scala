/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import auth._
import config.{FrontendAppConfig, FrontendAuthConnector}
import forms.NotEligibleForm
import models.NotEligibleModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class NotEligibleControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "NotEligibleController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showNotEligible" -> TestNotEligibleController.showNotEligible,
    "submitNotEligible" -> TestNotEligibleController.submitNotEligible
  )

  object TestNotEligibleController extends NotEligibleController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The NotEligible controller" should {
    "use the correct applicationConfig" in {
      NotEligibleController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      NotEligibleController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      NotEligibleController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showNotEligible action of the NotEligible controller with an authorised user" should {

    lazy val result = TestNotEligibleController.showNotEligible(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchNotEligible = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchNotEligible = 1, saveNotEligible = 0)
    }
  }

  "Calling the showNotEligible action of the NotEligible controller with an authorised user and valid submission" should {

    def callShow(option: String) = TestNotEligibleController.submitNotEligible(authenticatedFakeRequest()
      .post(NotEligibleForm.notEligibleForm, NotEligibleModel(option)))

    "return a not implemented status (501) for SignUp" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(NotEligibleForm.option_signup)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchNotEligible = 0, saveNotEligible = 1)
    }

    "return a not implemented status (501) for SignOut" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(NotEligibleForm.option_signout)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchNotEligible = 0, saveNotEligible = 1)
    }

  }

  "Calling the showNotEligible action of the NotEligible controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestNotEligibleController.submitNotEligible(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchNotEligible = 0, saveNotEligible = 0)
    }
  }


  authorisationTests
}
