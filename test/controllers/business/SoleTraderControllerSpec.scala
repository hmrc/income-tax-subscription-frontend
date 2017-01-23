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
import forms.SoleTraderForm
import models.SoleTraderModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class SoleTraderControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "SoleTraderController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSoleTrader" -> TestSoleTraderController.showSoleTrader,
    "submitSoleTrader" -> TestSoleTraderController.submitSoleTrader
  )

  object TestSoleTraderController extends SoleTraderController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The SoleTrader controller" should {
    "use the correct applicationConfig" in {
      SoleTraderController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      SoleTraderController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      SoleTraderController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showSoleTrader action of the SoleTrader with an authorised user" should {

    lazy val result = TestSoleTraderController.showSoleTrader(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchSoleTrader = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchSoleTrader = 1, saveSoleTrader = 0)
    }
  }

  "Calling the submitSoleTrader action of the SoleTrader with an authorised user and valid submission" should {

    def callShow(answer: String) = TestSoleTraderController.submitSoleTrader(authenticatedFakeRequest()
      .post(SoleTraderForm.soleTraderForm, SoleTraderModel(answer)))

    "return an SEE OTHER (303) for yes" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(SoleTraderForm.option_yes)

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest).get mustBe controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url

      await(goodRequest)
      verifyKeystore(fetchSoleTrader = 0, saveSoleTrader = 1)
    }

    "return a SEE OTHER (303) for no" in {
      setupMockKeystoreSaveFunctions()

      val goodRequest = callShow(SoleTraderForm.option_no)

      status(goodRequest) must be(Status.SEE_OTHER)
      redirectLocation(goodRequest).get mustBe controllers.routes.NotEligibleController.showNotEligible().url

      await(goodRequest)
      verifyKeystore(fetchSoleTrader = 0, saveSoleTrader = 1)
    }
  }

  "Calling the submitSoleTrader action of the SoleTrader with an authorised user and invalid submission" should {
    lazy val badRequest = TestSoleTraderController.submitSoleTrader(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchSoleTrader = 0, saveSoleTrader = 0)
    }
  }


  authorisationTests
}
