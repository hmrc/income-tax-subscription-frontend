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
import forms.BusinessNameForm
import models.BusinessNameModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService

class BusinessNameControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showBusinessName" -> TestBusinessNameController.showBusinessName(isEditMode = false),
    "submitBusinessName" -> TestBusinessNameController.submitBusinessName(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService
  )

  "Calling the showBusinessName action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.showBusinessName(isEditMode = false)(authenticatedFakeRequest())

    "return ok (200)" in {
      setupMockKeystore(fetchBusinessName = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessName = 1, saveBusinessName = 0)

    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean) =
      TestBusinessNameController.submitBusinessName(isEditMode = isEditMode)(
        authenticatedFakeRequest()
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }

      s"redirect to '${controllers.business.routes.BusinessAccountingMethodController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessAccountingMethodController.show().url)

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

      s"redirect to '${controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessName = 0, saveBusinessName = 1)
      }
    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submitBusinessName(isEditMode = false)(authenticatedFakeRequest())

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchAccountingPeriodDate = 0, saveAccountingPeriodDate = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url}" in {
      TestBusinessNameController.backUrl mustBe controllers.business.routes.BusinessAccountingPeriodDateController.showAccountingPeriod().url
    }
  }

  authorisationTests()

}
