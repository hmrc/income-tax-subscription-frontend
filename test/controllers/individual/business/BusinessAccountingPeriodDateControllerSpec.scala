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

package controllers.individual.business

import java.time.LocalDate

import assets.MessageLookup
import controllers.ControllerBaseSpec
import core.config.MockConfig
import core.config.featureswitch.FeatureSwitching
import services.individual.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels.testCacheMapCustom
import forms.individual.business.AccountingPeriodDateForm
import models.DateModel
import models.individual.business.AccountingPeriodModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockAccountingPeriodService

import scala.concurrent.Future

class BusinessAccountingPeriodDateControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching
  with MockAccountingPeriodService {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> testBusinessAccountingPeriodDateController.show(isEditMode = false, editMatch = false),
    "submit" -> testBusinessAccountingPeriodDateController.submit(isEditMode = false, editMatch = false)
  )

  lazy val testBusinessAccountingPeriodDateController: BusinessAccountingPeriodDateController =
    new BusinessAccountingPeriodDateController(
      mockBaseControllerConfig(new MockConfig {}),
      messagesApi,
      MockKeystoreService,
      mockAuthService,
      mockAccountingPeriodService
    )

  s"When the user is in the sign up journey, $controllerName" when {

    lazy val request = subscriptionRequest

    s"Calling the show action of $controllerName with an authorised user with a " +
      "journeyStateKey, Nino and UTR in session" should {

      lazy val result = testBusinessAccountingPeriodDateController.show(isEditMode = false, editMatch = false)(request)

      "return OK (200)" in {
        setupMockKeystore(fetchAccountingPeriodDate = None)
        status(result) must be(OK)
      }

      s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_signup}'" in {
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_signup
      }
    }

    s"Calling the submit action of $controllerName with an authorised user and a valid submission" should {

      def callSubmit(isEditMode: Boolean, accountingPeriodDates: AccountingPeriodModel): Future[Result] = testBusinessAccountingPeriodDateController
        .submit(isEditMode = isEditMode, editMatch = false)(request
          .post(AccountingPeriodDateForm.accountingPeriodDateForm, accountingPeriodDates)
        )

      "When it is not in edit mode" when {
        "the individual enters accounting period dates which meet the business validation rules" should {
          "return redirect status (SEE_OTHER - 303) and lead to the accounting-method page" in {

            val start = LocalDate.now
            val end = LocalDate.now.plusYears(1).minusDays(1)

            val testAccountingPeriodDates = AccountingPeriodModel(
              DateModel.dateConvert(start),
              DateModel.dateConvert(end)
            )

            setupMockKeystore(
              fetchRentUkProperty = TestModels.testRentUkProperty_no_property
            )

            mockCheckEligibleAccountingPeriod(start, end, hasPropertyIncomeSource = false)(eligible = true)

            val goodRequest = callSubmit(isEditMode = false, testAccountingPeriodDates)
            status(goodRequest) must be(SEE_OTHER)

            redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessAccountingMethodController.show().url)

            await(goodRequest)
            verifyKeystore(fetchRentUkProperty = 1, saveAccountingPeriodDate = 1)
          }
        }

        "the individual enters accounting period dates does not meet the business validation rules" should {
          "return a redirect status (SEE_OTHER - 303) and lead to cannot-use-service-yet page" in {
            val start = LocalDate.now
            val end = LocalDate.now.plusYears(2).minusDays(1)

            val testAccountingPeriodDates = AccountingPeriodModel(
              DateModel.dateConvert(start),
              DateModel.dateConvert(end)
            )

            setupMockKeystore(
              fetchRentUkProperty = TestModels.testRentUkProperty_no_property
            )

            mockCheckEligibleAccountingPeriod(start, end, hasPropertyIncomeSource = false)(eligible = false)

            val goodRequest = callSubmit(isEditMode = false, testAccountingPeriodDates)

            status(goodRequest) must be(SEE_OTHER)
            redirectLocation(goodRequest) mustBe Some(controllers.individual.eligibility.routes.NotEligibleForIncomeTaxController.show().url)

            await(goodRequest)
            verifyKeystore(fetchRentUkProperty = 1)
          }
        }
      }

      "When it is in edit mode" should {
        s"return a redirect status (SEE_OTHER - 303) and redirect to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          val start = LocalDate.now
          val end = LocalDate.now.plusYears(1).minusDays(1)

          val testAccountingPeriodDates = AccountingPeriodModel(
            DateModel.dateConvert(start),
            DateModel.dateConvert(end)
          )

          mockCheckEligibleAccountingPeriod(start, end, hasPropertyIncomeSource = false)(eligible = true)

          setupMockKeystore(
            fetchRentUkProperty = TestModels.testRentUkProperty_no_property,
            fetchAll = testCacheMapCustom(
              accountingPeriodDate = testAccountingPeriodDates)
          )

          val goodRequest = callSubmit(isEditMode = true, testAccountingPeriodDates)

          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)
          verifyKeystore(saveAccountingPeriodDate = 1)
        }
      }
    }

    s"Calling the submit action of $controllerName with an authorised user and invalid submission" should {
      lazy val badrequest = testBusinessAccountingPeriodDateController.submit(isEditMode = false, editMatch = false)(request)

      "return a bad request status (400)" in {
        // required for backurl
        setupMockKeystore(
          fetchAll = testCacheMapCustom(
            incomeSource = TestModels.testIncomeSourceBusiness)
        )

        status(badrequest) must be(BAD_REQUEST)

        await(badrequest)
        verifyKeystore(fetchAll = 0, saveAccountingPeriodDate = 0)
      }
    }

    "The back url for linear journey" should {
      s"point to ${controllers.individual.business.routes.MatchTaxYearController.show().url}" in {
        testBusinessAccountingPeriodDateController.backUrl(isEditMode = false, editMatch = false)(request) mustBe
          controllers.individual.business.routes.MatchTaxYearController.show().url
      }
    }

    "The back url for edit journey if editmatch is false" should {
      s"point to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        testBusinessAccountingPeriodDateController.backUrl(isEditMode = true, editMatch = false)(request) mustBe
          controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    }

    "The back url for edit journey if editmatch is true" should {
      s"point to ${controllers.individual.business.routes.MatchTaxYearController.show(editMode = true).url}" in {
        testBusinessAccountingPeriodDateController.backUrl(isEditMode = true, editMatch = true)(request) mustBe
          controllers.individual.business.routes.MatchTaxYearController.show(editMode = true).url
      }
    }

  }


  s"When the user is in the registration journey, $controllerName" when {

    lazy val request = registrationRequest

    "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user on a registration journey" should {

      lazy val result = testBusinessAccountingPeriodDateController.show(isEditMode = false, editMatch = false)(request)

      "return ok (200)" in {
        setupMockKeystore(fetchAccountingPeriodDate = None)

        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 0)

      }

      s"the rendered view should have the heading '${MessageLookup.AccountingPeriod.heading_registration}'" in {
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text mustBe MessageLookup.AccountingPeriod.heading_registration
      }
    }
  }

  authorisationTests()
}
