/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.agent.business

import agent.assets.MessageLookup
import agent.services.mocks.MockKeystoreService
import agent.utils.TestConstants
import agent.utils.TestModels._
import controllers.agent.AgentControllerBaseSpec
import core.services.mocks.MockAccountingPeriodService
import core.utils.TestModels
import forms.agent.AccountingPeriodDateForm
import incometax.util.CurrentDateProvider
import models.DateModel
import models.individual.business.AccountingPeriodModel
import org.jsoup.Jsoup
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._

import scala.concurrent.Future

class BusinessAccountingPeriodDateControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with MockAccountingPeriodService {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showSummary" -> TestBusinessAccountingPeriodController.show(isEditMode = false),
    "submitSummary" -> TestBusinessAccountingPeriodController.submit(isEditMode = false)
  )

  object DateProvider extends CurrentDateProvider

  object TestBusinessAccountingPeriodController extends BusinessAccountingPeriodDateController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockAccountingPeriodService,
    DateProvider
  )

  "show" should {
    "return the correct view" in {
      setupMockKeystore(fetchAccountingPeriodDate = None)
      lazy val result = await(TestBusinessAccountingPeriodController.show(isEditMode = false)(subscriptionRequest))

      status(result) mustBe OK
      verifyKeystore(fetchAccountingPeriodDate = 1)

      val document = Jsoup.parse(contentAsString(result))
      document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" when {
    val testAccountingPeriodDates = AccountingPeriodModel(DateModel dateConvert TestConstants.minStartDate,
      DateModel dateConvert TestConstants.minStartDate.plusYears(1))
    val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(DateModel dateConvert TestConstants.minStartDate,
      DateModel dateConvert TestConstants.minStartDate.plusYears(2))

    def callShow(isEditMode: Boolean, accountingPeriod: AccountingPeriodModel = testAccountingPeriodDates): Future[Result] =
      TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(AccountingPeriodDateForm.accountingPeriodDateForm, accountingPeriod))

    "When it is ineligible dates" should {
      s"return a redirect status (SEE_OTHER - 303) to kickout page" in {
        setupMockKeystore(
          fetchIncomeSource = Some(TestModels.testIncomeSourceBusiness),
          fetchAll = testCacheMap(
            accountingPeriodDate = Some(testAccountingPeriodDates)
          )
        )
        mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1),
          hasPropertyIncomeSource = false)(eligible = false)

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.eligibility.routes.NotEligibleForIncomeTaxController.show().url)
      }
    }

    "When it is not in edit mode and is eligible" when {
      "the tax year remained the same" should {
        s"return a redirect status (SEE_OTHER - 303) but do not update terms" in {
          setupMockKeystore(
            fetchIncomeSource = Some(TestModels.testIncomeSourceBusiness),
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDates)
            )
          )
          setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates)
          mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1),
            hasPropertyIncomeSource = false)(eligible = true)

          val goodRequest = await(callShow(isEditMode = false))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.BusinessAccountingMethodController.show().url)
          verifyKeystore(saveAccountingPeriodDate = 1)
        }
      }
      "the tax year changed" should {
        s"return a redirect status (SEE_OTHER - 303) and update terms" in {
          setupMockKeystore(
            fetchIncomeSource = Some(TestModels.testIncomeSourceBusiness),
            fetchAll = testCacheMap(
              accountingPeriodDate = Some(testAccountingPeriodDatesDifferentTaxYear)
            )
          )
          mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1),
            hasPropertyIncomeSource = false)(eligible = true)

          val goodRequest = await(callShow(isEditMode = false))

          status(goodRequest) mustBe SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.BusinessAccountingMethodController.show().url)
          verifyKeystore(saveAccountingPeriodDate = 1)
        }
      }
    }
    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystore(
          fetchIncomeSource = Some(TestModels.testIncomeSourceBusiness),
          fetchAll = testCacheMap(
            accountingPeriodDate = Some(testAccountingPeriodDates)
          )
        )
        mockCheckEligibleAccountingPeriod(TestConstants.minStartDate, TestConstants.minStartDate.plusYears(1), hasPropertyIncomeSource = false)(eligible = true)


        val goodRequest = await(callShow(isEditMode = true))

        status(goodRequest) mustBe SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)
        verifyKeystore(saveAccountingPeriodDate = 1)
      }
    }
  }

  "Calling the submitAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
    lazy val badrequest = await(TestBusinessAccountingPeriodController.submit(isEditMode = false)(subscriptionRequest))

    "return a bad request status (400)" in {
      // required for backurl
      setupMockKeystore(fetchIncomeSource = testIncomeSourceBusiness)

      status(badrequest) mustBe BAD_REQUEST
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "the user is submitting details for tax period" when {

    "The back url when it is not in edit mode" when {
      s"point to ${controllers.agent.business.routes.MatchTaxYearController.show().url}" in {
        TestBusinessAccountingPeriodController.backUrl(isEditMode = false) mustBe controllers.agent.business.routes.MatchTaxYearController.show().url
      }
    }

    "The back url when in edit mode" should {
      s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessAccountingPeriodController.backUrl(isEditMode = true) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }
    authorisationTests()

  }
}
