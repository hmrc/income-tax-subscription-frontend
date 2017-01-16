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

package controllers.business

import auth._
import config.{FrontendAppConfig, FrontendAuthConnector}
import controllers.ControllerBaseSpec
import forms.BusinessNameForm
import models.BusinessNameModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class BusinessNameControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessIncomeTypeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showBusinessIncomeType" -> TestBusinessNameController.showBusinessName,
    "submitBusinessIncomeType" -> TestBusinessNameController.submitBusinessName
  )

  object TestBusinessNameController extends BusinessNameController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The BusinessNameController controller" should {
    "use the correct applicationConfig" in {
      BusinessNameController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      BusinessNameController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      BusinessNameController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showBusinessName action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.showBusinessName(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchBusinessName = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessName = 1, saveBusinessName = 0)

    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and valid submission" should {

    def callShow = TestBusinessNameController.submitBusinessName(authenticatedFakeRequest().post(BusinessNameForm.businessNameForm, BusinessNameModel("Test business")))

    "return a redirect status (SEE_OTHER - 303)" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow

      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
    }

    s"redirect to '${controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url}'" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow

      redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessIncomeTypeController.showBusinessIncomeType().url)

      await(goodRequest)
      verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submitBusinessName(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 0)
    }
  }

  authorisationTests

}
