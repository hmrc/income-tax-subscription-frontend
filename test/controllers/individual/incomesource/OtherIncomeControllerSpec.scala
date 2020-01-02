/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.incomesource

import controllers.ControllerBaseSpec
import core.audit.Logging
import core.config.featureswitch.FeatureSwitching
import core.models.{No, Yes}
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels._
import incometax.incomesource.forms.OtherIncomeForm
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models.{Both, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class OtherIncomeControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with MockCurrentTimeService
  with FeatureSwitching {

  override val controllerName: String = "OtherIncomeController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOtherIncomeController.show(isEditMode = false),
    "submit" -> TestOtherIncomeController.submit(isEditMode = false)
  )

  object TestOtherIncomeController extends OtherIncomeController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    app.injector.instanceOf[Logging],
    mockAuthService,
    mockCurrentTimeService
  )

  Seq(false, true).foreach { editMode =>

    s"When in isEditMode=$editMode" that {

      "Calling the show action of the OtherIncome controller with an authorised user" should {
        def call = TestOtherIncomeController.show(isEditMode = editMode)(subscriptionRequest)

        "return ok (200) when there is valid income source in keystore" in {
          setupMockKeystore(fetchAll = testCacheMap)

          val result = call
          status(result) must be(Status.OK)
          await(result)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 0)
        }

        "return redirection (303) back to are you self-employed if there isn't income source in keystore" in {
          setupMockKeystore(fetchAll = None)

          val result = call
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
          await(result)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 0)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and saying yes to other income" when {
        def callSubmit = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, Yes))

        "there are no prior OtherIncome in the keystore then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = None))

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url}'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = None))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the same as the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = Yes))

          val goodRequest = callSubmit
          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        def expectedRedirectionForSameInput =
          if (editMode) controllers.individual.subscription.routes.CheckYourAnswersController.show().url
          else controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '$expectedRedirectionForSameInput'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = Yes))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the different from the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = No))

          val goodRequest = callSubmit
          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url}'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = No))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(controllers.individual.incomesource.routes.OtherIncomeErrorController.show().url)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and saying no to other income" when {

        def callSubmit = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, No))

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.individual.business.routes.BusinessNameController.show().url}' on the business journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.individual.subscription.routes.TermsController.show().url}' on the property 1 page journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_only,
              areYouSelfEmployed = None,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.TermsController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.individual.subscription.routes.TermsController.show().url}' on the property 2 pages journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_no,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.TermsController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${controllers.individual.business.routes.BusinessNameController.show().url}' on the both journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        def expectedRedirectionForSameInput(noneEditModeUrl: String) =
          if (editMode) controllers.individual.subscription.routes.CheckYourAnswersController.show().url
          else noneEditModeUrl

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(controllers.individual.business.routes.BusinessNameController.show().url)
        }' on the business journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = No
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(controllers.individual.business.routes.BusinessNameController.show().url))
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(controllers.individual.subscription.routes.TermsController.show().url)
        }' on the property journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_no,
              otherIncome = No
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(controllers.individual.subscription.routes.TermsController.show().url))

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(controllers.individual.business.routes.BusinessNameController.show().url)
        }' on the both journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = No
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(controllers.individual.business.routes.BusinessNameController.show().url))
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.individual.business.routes.BusinessNameController.show().url}' on the business journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = Yes
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }


        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.individual.subscription.routes.TermsController.show().url}' on the property journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_only,
              areYouSelfEmployed = None,
              otherIncome = Yes
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.TermsController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${controllers.individual.business.routes.BusinessNameController.show().url}' on the both journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              areYouSelfEmployed = TestModels.testAreYouSelfEmployed_yes,
              otherIncome = Yes
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and with an invalid choice" should {

        val dummy = "Invalid"

        def badrequest = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .postInvalid(OtherIncomeForm.otherIncomeForm, "dummy"))

        "return a bad request status (400)" in {
          setupMockKeystore(
            fetchAll = testCacheMap
          )

          val result = badrequest
          status(result) must be(Status.BAD_REQUEST)

          await(result)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 0)
        }
      }
    }
  }

  "The back url not in edit mode" should {
    s"point to ${controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url} on other income page" in {
      TestOtherIncomeController.backUrl(Both, isEditMode = false) mustBe controllers.individual.incomesource.routes.AreYouSelfEmployedController.show().url
    }
  }

  "The back url for the old income source mode in edit mode" when {
    s"point to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url} on other income page" in {
      TestOtherIncomeController.backUrl(Property, isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    }
  }

  "The back url in edit mode" should {
    s"point to ${
      controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    } on other income page" in {
      TestOtherIncomeController.backUrl(Both, isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()
}
