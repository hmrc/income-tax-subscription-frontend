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
import forms.AccountingPeriodForm
import models.{AccountingPeriodModel, DateModel}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class BusinessAccountingPeriodControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.showAccountingPeriod,
    "submitSummary" -> TestBusinessAccountingPeriodController.submitAccountingPeriod
  )

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodController {
    override lazy val applicationConfig = MockConfig
    override lazy val authConnector = MockAuthConnector
    override lazy val postSignInRedirectUrl = MockConfig.ggSignInContinueUrl
    override val keystoreService = MockKeystoreService
  }

  "The BusinessAccountingPeriod controller" should {
    "use the correct applicationConfig" in {
      BusinessAccountingPeriodController.applicationConfig must be(FrontendAppConfig)
    }
    "use the correct authConnector" in {
      BusinessAccountingPeriodController.authConnector must be(FrontendAuthConnector)
    }
    "use the correct postSignInRedirectUrl" in {
      BusinessAccountingPeriodController.postSignInRedirectUrl must be(FrontendAppConfig.ggSignInContinueUrl)
    }
  }

  "Calling the showAccountingPeriod action of the BusinessAccountingPeriod with an authorised user" should {

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(authenticatedFakeRequest())

    "return ok (200)" in {
      // required for backurl
      setupMockKeystore(fetchSoleTrader = TestModels.testIsSoleTrader)

      setupMockKeystore(fetchAccountingPeriod = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 1, saveAccountingPeriod = 0)

    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and a valid submission" should {

    def callShow = TestBusinessAccountingPeriodController.submitAccountingPeriod(authenticatedFakeRequest()
      .post(AccountingPeriodForm.accountingPeriodForm, AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))))

    "return a redirect status (SEE_OTHER - 303)" in {
      // required for backurl
      setupMockKeystore(fetchSoleTrader = TestModels.testIsSoleTrader)

      val goodRequest = callShow

      status(goodRequest) must be(Status.SEE_OTHER)

      await(goodRequest)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
    }

    s"redirect to '${controllers.business.routes.BusinessNameController.showBusinessName().url}'" in {
      // required for backurl
      setupMockKeystore(fetchSoleTrader = TestModels.testIsSoleTrader)

      val goodRequest = callShow

      redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.showBusinessName().url)

      await(goodRequest)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and invalid submission" should {
    lazy val badrequest = TestBusinessAccountingPeriodController.submitAccountingPeriod(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchSoleTrader = TestModels.testIsSoleTrader)

      status(badrequest) must be(Status.BAD_REQUEST)

      await(badrequest)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.SoleTraderController.showSoleTrader().url} if user answered yes to sole trader" in {
      setupMockKeystore(fetchSoleTrader = TestModels.testIsSoleTrader)
      await(TestBusinessAccountingPeriodController.backUrl(FakeRequest())) mustBe controllers.business.routes.SoleTraderController.showSoleTrader().url
      verifyKeystore(fetchSoleTrader = 1)
    }

    s"point to ${controllers.routes.NotEligibleController.showNotEligible().url} if user answered no to sole trader" in {
      setupMockKeystore(fetchSoleTrader = TestModels.testIsNotSoleTrader)
      await(TestBusinessAccountingPeriodController.backUrl(FakeRequest())) mustBe controllers.routes.NotEligibleController.showNotEligible().url
      verifyKeystore(fetchSoleTrader = 1)
    }
  }

  authorisationTests

}
