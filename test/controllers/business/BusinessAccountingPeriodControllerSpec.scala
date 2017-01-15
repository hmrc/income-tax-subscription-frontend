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
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

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
      setupMockKeystore(fetchAccountingPeriod = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 1, saveAccountingPeriod = 0)

    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and a valid entry" should {

    def callShow = TestBusinessAccountingPeriodController.submitAccountingPeriod(authenticatedFakeRequest()
      .post(AccountingPeriodForm.accountingPeriodForm, AccountingPeriodModel(DateModel("1", "4", "2017"), DateModel("1", "4", "2018"))))

    "return a redirect status (SEE_OTHER - 303)" in {
      setupMockKeystoreSaveFunctions()

      val result = callShow

      status(result) must be(Status.SEE_OTHER)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
    }

    s"redirect to '${controllers.business.routes.BusinessNameController.showBusinessName().url}'" in {
      setupMockKeystoreSaveFunctions()

      val result = callShow

      redirectLocation(result) mustBe Some(controllers.business.routes.BusinessNameController.showBusinessName().url)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 1)
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriod with an authorised user and invalid entry" should {
    lazy val result = TestBusinessAccountingPeriodController.submitAccountingPeriod(authenticatedFakeRequest())

    "return unimplemented (501)" in {
      status(result) must be(Status.NOT_IMPLEMENTED)

      await(result)
      verifyKeystore(fetchAccountingPeriod = 0, saveAccountingPeriod = 0)
    }
  }

  authorisationTests

}
