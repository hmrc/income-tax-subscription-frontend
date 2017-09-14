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

import controllers.ControllerBaseSpec
import forms.BusinessPhoneNumberForm
import models.BusinessPhoneNumberModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import services.mocks.MockKeystoreService
import utils.TestConstants._

class BusinessPhoneNumberControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessPhoneNumberController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessPhoneNumberController.show(isEditMode = false),
    "submit" -> TestBusinessPhoneNumberController.submit(isEditMode = false)
  )

  object TestBusinessPhoneNumberController extends BusinessPhoneNumberController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  "Calling the show action of the BusinessPhoneNumberController with an authorised user" should {

    lazy val result = TestBusinessPhoneNumberController.show(isEditMode = false)(fakeRequest)

    "return ok (200)" in {
      setupMockKeystore(fetchBusinessPhoneNumber = None)

      status(result) must be(Status.OK)

      await(result)
      verifyKeystore(fetchBusinessPhoneNumber = 1, saveBusinessPhoneNumber = 0)

    }
  }

  "Calling the submit action of the BusinessPhoneNumberController with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean) =
      TestBusinessPhoneNumberController.submit(isEditMode = isEditMode)(
        fakeRequest
          .post(BusinessPhoneNumberForm.businessPhoneNumberForm.form, BusinessPhoneNumberModel(testPhoneNumber))
      )

    "When it is not in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) must be(Status.NOT_IMPLEMENTED)

        await(goodRequest)
        verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
      }

      // TODO update to the business address page when it's implemented
      s"redirect to '${controllers.business.routes.BusinessAccountingMethodController.show().url}'" ignore {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
      }
    }

    "When it is in edit mode" should {
      "return a redirect status (SEE_OTHER - 303)" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)

        await(goodRequest)
        verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
      }

      s"redirect to '${controllers.routes.CheckYourAnswersController.show().url}'" in {
        setupMockKeystoreSaveFunctions()

        val goodRequest = callShow(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
      }
    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessPhoneNumberController.submit(isEditMode = false)(fakeRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.business.routes.BusinessNameController.showBusinessName().url}" in {
      TestBusinessPhoneNumberController.backUrl mustBe controllers.business.routes.BusinessNameController.showBusinessName().url
    }
  }

  authorisationTests()

}
