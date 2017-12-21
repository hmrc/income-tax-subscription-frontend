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

package agent.controllers

import agent.forms.IncomeSourceForm
import agent.models.IncomeSourceModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class IncomeSourceControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showIncomeSource" -> TestIncomeSourceController.show(isEditMode = true),
    "submitIncomeSource" -> TestIncomeSourceController.submit(isEditMode = true)
  )

  object TestIncomeSourceController extends IncomeSourceController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(subscriptionRequest)
      m must not be null
      m.apply("base.back") must be("Back")
    }
  }

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

    def callShow(option: String, isEditMode: Boolean) = TestIncomeSourceController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(IncomeSourceForm.incomeSourceForm, IncomeSourceModel(option))
    )

    "When it is not edit mode" should {
      s"return an SEE OTHER (303) for business and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_business, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for property and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_property, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_both, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.MainIncomeErrorController.show().url}" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(IncomeSourceForm.option_other, isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.MainIncomeErrorController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 0, saveIncomeSource = 1)
      }
    }

    "When it is in edit mode and user's selection has not changed" should {
      s"return an SEE OTHER (303) for business and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(IncomeSourceForm.option_business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for property and goto ${agent.controllers.routes.CheckYourAnswersController.show()}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceProperty)

        val goodRequest = callShow(IncomeSourceForm.option_property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(IncomeSourceForm.option_both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceOther)

        val goodRequest = callShow(IncomeSourceForm.option_other, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 0)
      }
    }

    "When it is in edit mode and user's selection has changed" should {
      s"return an SEE OTHER (303) for business and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(IncomeSourceForm.option_business, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for property and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(IncomeSourceForm.option_property, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for both and goto ${agent.controllers.routes.OtherIncomeController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBusiness)

        val goodRequest = callShow(IncomeSourceForm.option_both, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe agent.controllers.routes.OtherIncomeController.show().url

        await(goodRequest)
        verifyKeystore(fetchIncomeSource = 1, saveIncomeSource = 1)
      }

      s"return a SEE OTHER (303) for 'Other' and goto ${agent.controllers.routes.MainIncomeErrorController.show().url}" in {
        setupMockKeystore(fetchIncomeSource = TestModels.testIncomeSourceBoth)

        val goodRequest = callShow(IncomeSourceForm.option_other, isEditMode = true)

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
