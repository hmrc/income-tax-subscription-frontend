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

package agent.controllers

import agent.forms.IncomeSourceForm
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import core.config.featureswitch.FeatureSwitching
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class IncomeSourceControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with MockCurrentTimeService with FeatureSwitching {

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

  "Calling the showIncomeSource action of the IncomeSource controller with an authorised user" should {

    lazy val result = TestIncomeSourceController.show(isEditMode = true)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeSource = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and valid submission" should {

    def callShow(option: IncomeSourceType, isEditMode: Boolean) = TestIncomeSourceController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(IncomeSourceForm.incomeSourceForm, option)
    )

    "When it is not edit mode" when {
      "the income source is business" should {
        s"return a SEE_OTHER with a redirect location of ${agent.controllers.routes.OtherIncomeController.show().url}" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(Both, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

          await(goodRequest)
          verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
        }
      }

<<<<<<< HEAD
=======
      "the income source is property" when {
        "the tax deferral feature switch is on" when {
          "the current date is before 6 April 2018" should {
            s"return a SEE_OTHER with a redirect location of ${agent.controllers.routes.CannotReportYetController.show().url}" in {
              enable(TaxYearDeferralFeature)
              mockGetTaxYearEnd(2018)
              setupMockKeystoreSaveFunctions()

              val goodRequest = callShow(Property, isEditMode = false)

              status(goodRequest) must be(Status.SEE_OTHER)
              redirectLocation(goodRequest) must contain(agent.controllers.routes.CannotReportYetController.show().url)

              await(goodRequest)
              verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
            }
          }
          "the current date is after 6 April 2018" should {
            s"return a SEE OTHER with a redirect location of ${agent.controllers.routes.OtherIncomeController.show().url}" in {
              enable(TaxYearDeferralFeature)
              mockGetTaxYearEnd(2019)
              setupMockKeystoreSaveFunctions()

              val goodRequest = callShow(Property, isEditMode = false)

              status(goodRequest) must be(Status.SEE_OTHER)
              redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

              await(goodRequest)
              verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
            }
          }
        }
        "the tax deferral feature switch is off" should {
          s"return a SEE OTHER with a redirect location of ${agent.controllers.routes.OtherIncomeController.show().url}" in {
            disable(TaxYearDeferralFeature)
            setupMockKeystoreSaveFunctions()

            val goodRequest = callShow(Property, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

            await(goodRequest)
            verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
          }
        }
      }

>>>>>>> 73b292ff4ddbc83ea9af7014a16b956f4a549aca

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(Both, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.MainIncomeErrorController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(Other, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.MainIncomeErrorController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }
    }

    "When it is in edit mode and user's selection has not changed" should {
      s"return an SEE OTHER (303) for business and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(Business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for property and goto ${agent.controllers.routes.CheckYourAnswersController.show()}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

        val goodRequest = callShow(Property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceOther)

        val goodRequest = callShow(Other, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }
    }

    "When it is in edit mode and user's selection has changed" should {
      s"return an SEE OTHER (303) for business and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for property and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(Both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.MainIncomeErrorController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(Other, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.MainIncomeErrorController.show().url

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
    s"point to ${agent.controllers.routes.CheckYourAnswersController.show().url} on income source page" in {
      TestIncomeSourceController.backUrl mustBe agent.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
