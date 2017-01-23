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
import forms.IncomeSourceForm
import models.IncomeSourceModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class IncomeSourceControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showIncomeSource" -> TestIncomeSourceController.showIncomeSource,
    "submitIncomeSource" -> TestIncomeSourceController.submitIncomeSource
  )

  object TestIncomeSourceController extends IncomeSourceController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The IncomeSource controller" should {
    "use the correct applicationConfig" in {
      IncomeSourceController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      IncomeSourceController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      IncomeSourceController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showIncomeSource action of the IncomeSource controller with an authorised user" should {

    lazy val result = TestIncomeSourceController.showIncomeSource(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeSource = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and valid submission" should {

    def callShow(option: String) = TestIncomeSourceController.submitIncomeSource(authenticatedFakeRequest()
      .post(IncomeSourceForm.incomeSourceForm, IncomeSourceModel(option)))

    "return an unimplemented (501) for business" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(IncomeSourceForm.option_business)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
    }

    "return a SEE OTHER (300) for property" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(IncomeSourceForm.option_property)

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest).get mustBe controllers.property.routes.PropertyIncomeController.submitPropertyIncome().url

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
    }

    "return an unimplemented (501) for both" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(IncomeSourceForm.option_both)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
    }

  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestIncomeSourceController.submitIncomeSource(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 0)
    }
  }

  authorisationTests

}
