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

import core.ITSASessionKeys
import core.auth.Registration
import core.config.MockConfig
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException
import core.utils.TestConstants._
import incometax.business.forms.BusinessPhoneNumberForm
import incometax.business.models.BusinessPhoneNumberModel

class BusinessPhoneNumberControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "BusinessPhoneNumberController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessPhoneNumberController.show(isEditMode = false),
    "submit" -> TestBusinessPhoneNumberController.submit(isEditMode = false)
  )

  def createTestBusinessPhoneNumberController(setEnableRegistration: Boolean): BusinessPhoneNumberController =
    new BusinessPhoneNumberController(
      mockBaseControllerConfig(new MockConfig {
        override val enableRegistration = setEnableRegistration
      }),
      messagesApi,
      MockKeystoreService,
      mockAuthService
    )

  lazy val TestBusinessPhoneNumberController: BusinessPhoneNumberController =
    createTestBusinessPhoneNumberController(setEnableRegistration = true)

  lazy val request = subscriptionRequest.withSession(ITSASessionKeys.JourneyStateKey -> Registration.name)

  "When registration is disabled" should {
    lazy val TestBusinessPhoneNumberController: BusinessPhoneNumberController =
      createTestBusinessPhoneNumberController(setEnableRegistration = false)

    "show" should {
      "return NOT FOUND" in {
        val result = TestBusinessPhoneNumberController.show(isEditMode = true)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet available to the public:")
      }
    }

    "submit" should {
      "return NOT FOUND" in {
        val result = TestBusinessPhoneNumberController.submit(isEditMode = true)(request)
        val ex = intercept[NotFoundException] {
          await(result)
        }
        ex.message must startWith("This page for registration is not yet available to the public:")
      }
    }
  }

  "When registration is enabled" should {

    "Calling the show action of the BusinessPhoneNumberController with an authorised user" should {

      lazy val result = TestBusinessPhoneNumberController.show(isEditMode = false)(request)

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
          request
            .post(BusinessPhoneNumberForm.businessPhoneNumberForm.form, BusinessPhoneNumberModel(testPhoneNumber))
        )

      "When it is not in edit mode" should {
        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
        }

        s"redirect to '${incometax.business.controllers.routes.BusinessAddressController.show().url}'" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = false)

          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessAddressController.show().url)

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

        s"redirect to '${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}'" in {
          setupMockKeystoreSaveFunctions()

          val goodRequest = callShow(isEditMode = true)

          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.CheckYourAnswersController.show().url)

          await(goodRequest)
          verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 1)
        }
      }
    }

    "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
      lazy val badRequest = TestBusinessPhoneNumberController.submit(isEditMode = false)(request)

      "return a bad request status (400)" in {
        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyKeystore(fetchBusinessPhoneNumber = 0, saveBusinessPhoneNumber = 0)
      }
    }

    "The back url when not in edit mode" should {
      s"point to ${incometax.business.controllers.routes.BusinessNameController.show().url}" in {
        TestBusinessPhoneNumberController.backUrl(isEditMode = false) mustBe incometax.business.controllers.routes.BusinessNameController.show().url
      }
    }

    "The back url when in edit mode" should {
      s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessPhoneNumberController.backUrl(isEditMode = true) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()

}
