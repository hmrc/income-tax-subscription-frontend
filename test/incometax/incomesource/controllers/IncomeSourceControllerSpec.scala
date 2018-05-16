/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.controllers

import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models.{Both, Business, IncomeSourceType, Property}
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class IncomeSourceControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestIncomeSourceController.show(isEditMode = true),
    "submit" -> TestIncomeSourceController.submit(isEditMode = true)
  )

  object TestIncomeSourceController extends IncomeSourceController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockCurrentTimeService
  )


  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(subscriptionRequest)
      m must not be null
      m.apply("base.back") must be("Back")
    }
  }

  "Calling the showIncomeSource action of the IncomeSource controller with an authorised user" when {

    def call = TestIncomeSourceController.show(isEditMode = true)(subscriptionRequest)

    "the new income source flow feature is enabled" should {
      "return not found (404)" in {
        enable(NewIncomeSourceFlowFeature)
        intercept[NotFoundException](await(call))
      }
    }

    "the new income source flow feature is disabled" should {
      "return ok (200)" in {
        disable(NewIncomeSourceFlowFeature)
        setupMockKeystore(fetchIncomeSource = None)

        val result = call
        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and valid submission" should {

    def callShow(option: IncomeSourceType, isEditMode: Boolean) = TestIncomeSourceController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(IncomeSourceForm.incomeSourceForm, option))

    "When it is not edit mode" should {
      s"return an SEE OTHER (303) for business and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(Business, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

<<<<<<< HEAD
      "when the current date is after the 2017 - 2018 tax year" should {
=======
      "when the current date is within the 2017 - 2018 tax year" when {
        "tax year deferral is disabled" should {
          s"return a SEE OTHER (303) for property and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
            disable(TaxYearDeferralFeature)

            setupMockKeystoreSaveFunctions()
            mockGetTaxYearEnd(2018)

            val goodRequest = callShow(Property, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

            await(goodRequest)
            verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
          }
        }

        "tax year deferral is enabled" should {
          s"return a SEE OTHER (303) for property and goto ${incometax.incomesource.controllers.routes.CannotReportYetController.show().url}" in {
            enable(TaxYearDeferralFeature)

            setupMockKeystoreSaveFunctions()
            mockGetTaxYearEnd(2018)

            val goodRequest = callShow(Property, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.CannotReportYetController.show().url

            await(goodRequest)
            verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
          }
        }
      }

      "when the current date is after the 2017 - 2018 tax year" should {
        "tax year deferral is disable" should {
          s"return a SEE OTHER (303) for property and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
            disable(TaxYearDeferralFeature)

            setupMockKeystoreSaveFunctions()
            mockGetTaxYearEnd(2019)

            val goodRequest = callShow(Property, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

            await(goodRequest)
            verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
          }
        }

        "tax year deferral is enable" should {
>>>>>>> 73b292ff4ddbc83ea9af7014a16b956f4a549aca
          s"return a SEE OTHER (303) for property and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {

            setupMockKeystoreSaveFunctions()
            mockGetTaxYearEnd(2019)

            val goodRequest = callShow(Property, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

            await(goodRequest)
            verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
        }
      }

      s"return a SEE OTHER (303) for both and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(Both, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }
    }

    "When it is in edit mode and user's selection has not changed" should {
      s"return an SEE OTHER (303) for business and goto ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(Business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for property and goto ${incometax.subscription.controllers.routes.CheckYourAnswersController.show()}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

        val goodRequest = callShow(Property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for both and goto ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }
    }

    "When it is in edit mode and user's selection has changed" should {
      s"return an SEE OTHER (303) for business and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for property and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for both and goto ${incometax.incomesource.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(Both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestIncomeSourceController.submit(isEditMode = true)(subscriptionRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 0)
    }
  }

  "The back url" should {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url} on income source page" in {
      TestIncomeSourceController.backUrl mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
