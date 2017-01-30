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
import controllers.ControllerBaseSpec
import forms.IncomeTypeForm
import models.IncomeTypeModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class BusinessIncomeTypeControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessIncomeTypeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showBusinessIncomeType" -> TestBusinessIncomeTypeController.showBusinessIncomeType(isEditMode = false),
    "submitBusinessIncomeType" -> TestBusinessIncomeTypeController.submitBusinessIncomeType(isEditMode = false)
  )

  object TestBusinessIncomeTypeController extends BusinessIncomeTypeController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showBusinessIncomeType action of the BusinessIncomeType with an authorised user" should {

    lazy val result = TestBusinessIncomeTypeController.showBusinessIncomeType(isEditMode = false)(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchIncomeType = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchIncomeType = 1, saveIncomeType = 0)
    }
  }

  "Calling the submitBusinessIncomeType action of the BusinessIncomeType with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean) = TestBusinessIncomeTypeController.submitBusinessIncomeType(isEditMode = isEditMode)(authenticatedFakeRequest()
      .post(IncomeTypeForm.incomeTypeForm, IncomeTypeModel(IncomeTypeForm.option_cash)))

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchIncomeType = 0, saveIncomeType = 1)
      }

      s"redirect to '${controllers.routes.ContactEmailController.showContactEmail().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.ContactEmailController.showContactEmail().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeType = 0, saveIncomeType = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchIncomeType = 0, saveIncomeType = 1)
      }

      s"redirect to '${controllers.routes.SummaryController.showSummary().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.SummaryController.showSummary().url)

        await(goodRequest)
        verifyKeystore(fetchIncomeType = 0, saveIncomeType = 1)
      }
    }
  }

  "Calling the submitBusinessIncomeType action of the BusinessIncomeType with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessIncomeTypeController.submitBusinessIncomeType(isEditMode = false)(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchIncomeType = 0, saveIncomeType = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.BusinessNameController.showBusinessName().url}" in {
      TestBusinessIncomeTypeController.backUrl mustBe controllers.business.routes.BusinessNameController.showBusinessName().url
    }
  }

  authorisationTests

}
