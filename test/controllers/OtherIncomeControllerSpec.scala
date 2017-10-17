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

package controllers

import core.audit.Logging
import core.services.mocks.MockKeystoreService
import forms.OtherIncomeForm
import models.OtherIncomeModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import utils.TestModels

class OtherIncomeControllerSpec extends ControllerBaseSpec
  with MockKeystoreService {

  override val controllerName: String = "OtherIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "showOtherIncome" -> TestOtherIncomeController.showOtherIncome(isEditMode = false),
    "submitOtherIncome" -> TestOtherIncomeController.submitOtherIncome(isEditMode = false)
  )

  object TestOtherIncomeController extends OtherIncomeController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService
  )

  Seq(false, true).foreach { editMode =>

    s"When in isEditMode=$editMode" that {

      "Calling the showOtherIncome action of the OtherIncome controller with an authorised user" should {
        lazy val result = TestOtherIncomeController.showOtherIncome(isEditMode = editMode)(subscriptionRequest)

        "return ok (200)" in {
          setupMockKeystore(fetchOtherIncome = None)

          status(result) must be(Status.OK)

          await(result)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 0)
        }
      }

      "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying yes to other income" when {
        def callSubmit = TestOtherIncomeController.submitOtherIncome(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_yes)))

        "there are no prior OtherIncome in the keystore then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchOtherIncome = None)

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url}'" in {
          setupMockKeystore(fetchOtherIncome = None)

          val goodRequest = callSubmit

          redirectLocation(goodRequest) mustBe Some(controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the same as the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_yes))

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }

        def expectedRedirectionForSameInput =
          if (editMode) incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          else controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '$expectedRedirectionForSameInput'" in {
          setupMockKeystore(fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_yes))

          val goodRequest = callSubmit

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the different from the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no))

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url}'" in {
          setupMockKeystore(fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no))

          val goodRequest = callSubmit

          redirectLocation(goodRequest) mustBe Some(controllers.routes.OtherIncomeErrorController.showOtherIncomeError().url)

          await(goodRequest)
          verifyKeystore(fetchOtherIncome = 1, saveOtherIncome = 1)
        }
      }

      "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and saying no to other income" should {

        def callSubmit = TestOtherIncomeController.submitOtherIncome(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_no)))

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.business.routes.BusinessNameController.show().url}' on the business journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBusiness,
            fetchOtherIncome = None
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.subscription.controllers.routes.TermsController.showTerms().url}' on the property journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceProperty,
            fetchOtherIncome = None
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.showTerms().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.business.routes.BusinessNameController.show().url}' on the both journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBoth,
            fetchOtherIncome = None
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }

        def expectedRedirectionForSameInput(noneEditModeUrl: String) =
          if (editMode) incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          else noneEditModeUrl

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(controllers.business.routes.BusinessNameController.show().url)
        }' on the business journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBusiness,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(controllers.business.routes.BusinessNameController.show().url))

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = if (editMode) 0 else 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(incometax.subscription.controllers.routes.TermsController.showTerms().url)
        }' on the property journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceProperty,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(incometax.subscription.controllers.routes.TermsController.showTerms().url))

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = if (editMode) 0 else 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(controllers.business.routes.BusinessAccountingPeriodPriorController.show().url)
        }' on the both journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBoth,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(controllers.business.routes.BusinessNameController.show().url))

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = if (editMode) 0 else 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.business.routes.BusinessNameController.show().url}' on the business journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBusiness,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }


        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${incometax.subscription.controllers.routes.TermsController.showTerms().url}' on the property journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceProperty,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.showTerms().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.business.routes.BusinessNameController.show().url}' on the both journey" in {

          setupMockKeystore(
            fetchIncomeSource = TestModels.testIncomeSourceBoth,
            fetchOtherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(controllers.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchIncomeSource = 1)
        }
      }

      "Calling the submitOtherIncome action of the OtherIncome controller with an authorised user and with an invalid choice" should {

        val dummy = "Invalid"

        def badrequest = TestOtherIncomeController.submitOtherIncome(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(dummy)))

        "return a bad request status (400)" in {
          setupMockKeystoreSaveFunctions()

          status(badrequest) must be(Status.BAD_REQUEST)

          await(badrequest)
          verifyKeystore(fetchOtherIncome = 0, saveOtherIncome = 0)
        }

      }
    }

  }


  "The back url not in edit mode" should {
    s"point to ${controllers.routes.IncomeSourceController.showIncomeSource().url} on other income page" in {
      TestOtherIncomeController.backUrl(isEditMode = false) mustBe controllers.routes.IncomeSourceController.showIncomeSource().url
    }
  }

  "The back url in edit mode" should {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url} on other income page" in {
      TestOtherIncomeController.backUrl(isEditMode = true) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()
}
