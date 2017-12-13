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

package agent.controllers.business

import agent.assets.MessageLookup
import agent.controllers.AgentControllerBaseSpec
import agent.forms.AccountingPeriodDateForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.models.DateModel
import incometax.business.models.AccountingPeriodModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class BusinessAccountingPeriodDateControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)
  )

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodDateController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user with is current period as yes" should {

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false)(subscriptionRequest)

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

    lazy val result = TestBusinessAccountingPeriodController.showAccountingPeriod(isEditMode = false)(subscriptionRequest)

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
    val testAccountingPeriodDates = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2018"))
    val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(DateModel dateConvert AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2019"))

    def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = isEditMode)(subscriptionRequest
      .post(AccountingPeriodDateForm.accountingPeriodDateForm, testAccountingPeriodDates))

    "When it is not in edit mode" should {
      "the tax year remained the same" should {
        s"return a redirect status (SEE_OTHER - 303) but do not update terms" in {
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates, fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.showBusinessName().url)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1, saveTerms = 0, fetchAccountingPeriodPrior = 1)
        }
      }

      "the tax year changed" should {
        s"return a redirect status (SEE_OTHER - 303) and update terms" in {
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear, fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessNameController.showBusinessName().url)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1, saveTerms = 1, fetchAccountingPeriodPrior = 1)
        }
      }
    }

    "When it is in edit mode" should {
      "tax year remains the same" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates, fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)

          val goodRequest = callShow(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1, fetchAccountingPeriodPrior = 1)
        }
      }

      "tax year changes" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear, fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)

          val goodRequest = callShow(isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.TermsController.showTerms(editMode = true).url)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1, fetchAccountingPeriodPrior = 1)
        }
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
    lazy val badrequest = TestBusinessAccountingPeriodController.submitAccountingPeriod(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness, fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

      status(badrequest) must be(Status.BAD_REQUEST)

      await(badrequest)
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "The back url when the user is submitting details for current period" should {
    s"point to ${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }

  "The back url when the user is submitting details for next period" should {
    s"point to ${agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url}" in {
      setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(FakeRequest())) mustBe agent.controllers.business.routes.RegisterNextAccountingPeriodController.show().url
      verifyKeystore(fetchAccountingPeriodPrior = 1)
    }
  }
  "The back url when in edit mode" should {
    s"point to ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
      await(TestBusinessAccountingPeriodController.backUrl(isEditMode = true)(FakeRequest())) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()
}