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

package incometax.business.controllers

import assets.MessageLookup
import core.config.MockConfig
import core.controllers.ControllerBaseSpec
import core.models.DateModel
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import incometax.business.forms.AccountingPeriodDateForm
import incometax.business.models.AccountingPeriodModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class BusinessAccountingPeriodDateControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessAccountingPeriodDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessAccountingPeriodController.show(isEditMode = false),
    "submit" -> TestBusinessAccountingPeriodController.submit(isEditMode = false)
  )

  def createTestBusinessAccountingPeriodController(setEnableRegistration: Boolean): BusinessAccountingPeriodDateController =
    new BusinessAccountingPeriodDateController(
      mockBaseControllerConfig(new MockConfig {
        override val enableRegistration = setEnableRegistration
      }),
      messagesApi,
      MockKeystoreService,
      mockAuthService
    )

  lazy val TestBusinessAccountingPeriodController: BusinessAccountingPeriodDateController =
    createTestBusinessAccountingPeriodController(setEnableRegistration = false)

  "When the user is in the registration journey, BusinessAccountingPeriodDateController" when {
    lazy val TestBusinessAccountingPeriodController: BusinessAccountingPeriodDateController =
      createTestBusinessAccountingPeriodController(setEnableRegistration = true)

    lazy val request = registrationRequest

    "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate" should {

      lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false)(request)

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

    "Calling the submit action of the BusinessAccountingPeriodDate with a valid submission" should {

      val testAccountingPeriodDates = AccountingPeriodModel(AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2018"))
      val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2019"))

      def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode)(request
        .post(AccountingPeriodDateForm.accountingPeriodDateForm, testAccountingPeriodDates))

      "When it is not in edit mode" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)

          verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
        }
      }

      "When it is in edit mode" when {

        "tax year remains the same" should {
          "return a redirect status (SEE_OTHER - 303)" in {
            setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDates)

            val goodRequest = callShow(isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

            await(goodRequest)
            verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1)
          }
        }

        "tax year changes" should {
          "return a redirect status (SEE_OTHER - 303)" in {
            setupMockKeystore(fetchAccountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear)

            val goodRequest = callShow(isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.showTerms(editMode = true).url)

            await(goodRequest)
            verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1)
          }
        }

      }
    }

    "Calling the submit action of the BusinessAccountingPeriodDate with an invalid submission" should {
      lazy val badrequest = TestBusinessAccountingPeriodController.submit(isEditMode = false)(request)

      "return a bad request status (400)" in {
        status(badrequest) must be(Status.BAD_REQUEST)

        await(badrequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
      }
    }

    "The back url" should {
      s"point to ${incometax.business.controllers.routes.BusinessStartDateController.show().url}" in {
        await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(request)) mustBe incometax.business.controllers.routes.BusinessStartDateController.show().url
      }
    }

    "The back url in edit mode" should {
      s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        await(TestBusinessAccountingPeriodController.backUrl(isEditMode = true)(request)) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
      }
    }
  }

  "When the user is in the sign up journey, BusinessAccountingPeriodDateController" when {

    lazy val request = subscriptionRequest

    "Calling the showAccountingPeriod action of the BusinessAccountingPeriodDate with an authorised user with is current period as yes" should {

      lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false)(request)

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

    "Calling the show action of the BusinessAccountingPeriodDate with an authorised user with is current period prior as no" should {

      lazy val result = TestBusinessAccountingPeriodController.show(isEditMode = false)(request)

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

    "Calling the submit action of the BusinessAccountingPeriodDate with an authorised user and a valid submission" should {

      val testAccountingPeriodDates = AccountingPeriodModel(AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2018"))
      val testAccountingPeriodDatesDifferentTaxYear = AccountingPeriodModel(AccountingPeriodDateForm.minStartDate, DateModel("5", "4", "2019"))

      def callShow(isEditMode: Boolean) = TestBusinessAccountingPeriodController.submit(isEditMode = isEditMode)(request
        .post(AccountingPeriodDateForm.accountingPeriodDateForm, testAccountingPeriodDates))

      "When it is not in edit mode" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          // required for backurl
          setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
        }

        s"redirect to '${incometax.business.controllers.routes.BusinessAccountingMethodController.show().url}'" in {
          // required for backurl
          setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 1)
        }
      }

      "When it is in edit mode" when {

        "the tax year remained the same" should {
          s"return a redirect status (SEE_OTHER - 303) and redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
            setupMockKeystore(
              fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod, // required for backurl
              fetchAccountingPeriodDate = testAccountingPeriodDates
            )

            val goodRequest = callShow(isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            await(goodRequest)

            redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)
            verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1)
          }
        }

        "the tax year changed" should {
          s"return a redirect status (SEE_OTHER - 303) and redirect to '${
            incometax.subscription.controllers.routes.TermsController.showTerms(editMode = true).url
          }" in {
            setupMockKeystore(
              fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod, // required for backurl
              fetchAccountingPeriodDate = testAccountingPeriodDatesDifferentTaxYear
            )

            val goodRequest = callShow(isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            await(goodRequest)

            redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.showTerms(editMode = true).url)
            verifyKeystore(fetchAccountingPeriodDate = 1, saveAccountingPeriodDate = 1)
          }
        }

      }
    }

    "Calling the submit action of the BusinessAccountingPeriodDate with an authorised user and invalid submission" should {
      lazy val badrequest = TestBusinessAccountingPeriodController.submit(isEditMode = false)(request)

      "return a bad request status (400)" in {
        // required for backurl
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness, fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)

        status(badrequest) must be(Status.BAD_REQUEST)

        await(badrequest)
        verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
      }
    }

    "The back url when the user is submitting details for current period" should {
      s"point to ${incometax.business.controllers.routes.BusinessAccountingPeriodPriorController.show().url}" in {
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsCurrentPeriod)
        await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(request)) mustBe incometax.business.controllers.routes.BusinessAccountingPeriodPriorController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1)
      }
    }

    "The back url when the user is submitting details for next period" should {
      s"point to ${incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url}" in {
        setupMockKeystore(fetchAccountingPeriodPrior = TestModels.testIsNextPeriod)
        await(TestBusinessAccountingPeriodController.backUrl(isEditMode = false)(request)) mustBe incometax.business.controllers.routes.RegisterNextAccountingPeriodController.show().url
        verifyKeystore(fetchAccountingPeriodPrior = 1)
      }
    }

  }

  authorisationTests()
}