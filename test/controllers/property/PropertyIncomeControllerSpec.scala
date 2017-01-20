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

package controllers.property

import auth._
import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.ControllerBaseSpec
import forms.PropertyIncomeForm
import models.PropertyIncomeModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class PropertyIncomeControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "PropertyIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showPropertyIncome" -> TestPropertyIncomeController.showPropertyIncome,
    "submitPropertyIncome" -> TestPropertyIncomeController.submitPropertyIncome
  )

  object TestPropertyIncomeController extends PropertyIncomeController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The PropertyIncome controller" should {
    "use the correct applicationConfig" in {
      PropertyIncomeController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      PropertyIncomeController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      PropertyIncomeController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showIncomeSource action of the PropertyIncome controller with an authorised user" should {

    lazy val result = TestPropertyIncomeController.showPropertyIncome(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchPropertyIncome = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchPropertyIncome = 1, savePropertyIncome = 0)
    }
  }

  "Calling the submitPropertyIncome action of the PropertyIncome controller with an authorised user and valid submission" should {

    def callShow(option: String) = TestPropertyIncomeController.submitPropertyIncome(authenticatedFakeRequest()
      .post(PropertyIncomeForm.propertyIncomeForm, PropertyIncomeModel(option)))

    "return an unimplemented (501) for less than 10k" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(PropertyIncomeForm.option_LT10k)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchPropertyIncome = 0, savePropertyIncome = 1)
    }

    "return an unimplemented (501) for 10k or more" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(PropertyIncomeForm.option_GE10k)

      status(goodRequest) must be(Status.NOT_IMPLEMENTED)

      await(goodRequest)
      verifyKeystore(fetchPropertyIncome = 0, savePropertyIncome = 1)
    }

  }

  "Calling the submitPropertyIncome action of the PropertyIncome controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestPropertyIncomeController.submitPropertyIncome(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchPropertyIncome = 0, savePropertyIncome = 0)
    }
  }

  authorisationTests

}
