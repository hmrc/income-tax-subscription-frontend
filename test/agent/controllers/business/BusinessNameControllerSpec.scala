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

package agent.controllers.business

import agent.controllers.AgentControllerBaseSpec
import agent.forms.BusinessNameForm
import agent.models.BusinessNameModel
import agent.services.mocks.MockKeystoreService
import agent.utils.TestModels._
import core.config.featureswitch.FeatureSwitching
import core.models.DateModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models._
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class BusinessNameControllerSpec extends AgentControllerBaseSpec
  with MockKeystoreService with MockCurrentTimeService with FeatureSwitching {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showBusinessName" -> TestBusinessNameController.show(isEditMode = false),
    "submitBusinessName" -> TestBusinessNameController.submit(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockCurrentTimeService
  )

  "Calling the showBusinessName action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      setupMockKeystore(
        fetchBusinessName = None,
        fetchAccountingPeriodDate = Some(testAccountingPeriod),
        fetchIncomeSource = Some(testIncomeSourceBusiness)
      )

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessName = 1, saveBusinessName = 0)

    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean) =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }

      s"redirect to '${agent.controllers.business.routes.BusinessAccountingMethodController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(agent.controllers.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }

      s"redirect to '${agent.controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      setupMockKeystore(
        fetchAccountingPeriodDate = Some(testAccountingPeriod),
        fetchIncomeSource = Some(testIncomeSourceBusiness)
      )

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
    }

    "in edit mode" should {
      s"point to ${agent.controllers.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessNameController.backUrl(
          isEditMode = true,
          testAccountingPeriod,
          Business
        ) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()

}
