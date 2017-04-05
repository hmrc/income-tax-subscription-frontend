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

import assets.MessageLookup
import auth._
import controllers.ControllerBaseSpec
import forms.AccountingPeriodDateForm
import models.{AccountingPeriodModel, DateModel}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestModels

class BusinessAccountingPeriodDateControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)
  )

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodDateController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user with is current period as yes" should {

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false)(authenticatedFakeRequest())

    "return ok (200)" in {
      // required for backurl
      setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)

    }

    s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_current}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_current
    }
  }

  "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user with is current period prior as no" should {

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false)(authenticatedFakeRequest())

    "return ok (200)" in {
      // required for backurl
      setupMockKeystore(fetchAccountingPeriodDate = None, fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0, fetchAccountingPeriodPrior = 2)

    }

    s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_next}'" in {
      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_next
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" should {

    def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = isEditMode)(authenticatedFakeRequest()
      .post(AccountingPeriodDateForm.accountingPeriodDateForm, AccountingPeriodModel(AccountingPeriodDateForm.minStartDate, DateModel("1", "4", "2018"))))

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        // required for backurl
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
      }

      s"redirect to '${controllers.business.routes.BusinessNameController.showBusinessName().url}'" in {
        // required for backurl
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.showBusinessName().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        // required for backurl
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
      }

      s"redirect to '${controllers.routes.CheckYourAnswersController.show().url}'" in {
        // required for backurl
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
    lazy val badrequest = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness, fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

      status(badrequest) must be(Status.BAD_REQUEST)

      await(badrequest)
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "The back url when the user is submitting details for current period" should {
    s"point to ${controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)
      await(TestBusinessAccountingPeriodController.backUrl(FakeRequest())) mustBe controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }

  "The back url when the user is submitting details for next period" should {
    s"point to ${controllers.business.routes.RegisterNextAccountingPeriodController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)
      await(TestBusinessAccountingPeriodController.backUrl(FakeRequest())) mustBe controllers.business.routes.RegisterNextAccountingPeriodController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }
    authorisationTests()
  }