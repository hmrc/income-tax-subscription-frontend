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

import core.audit.Logging
import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature, TaxYearDeferralFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels
import core.utils.TestModels._
import incometax.incomesource.forms.OtherIncomeForm
import incometax.incomesource.models.OtherIncomeModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import incometax.subscription.models.{Both, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class OtherIncomeControllerNewIncomeSourceSpec extends ControllerBaseSpec
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

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
    enable(NewIncomeSourceFlowFeature)
  }

  override def afterEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
    disable(NewIncomeSourceFlowFeature)
  }

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

        "return redirection (303) back to work for yourself if there isn't income source in keystore" in {
          setupMockKeystore(fetchAll = None)

          val result = call
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
          await(result)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 0)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and saying yes to other income" when {
        def callSubmit = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_yes)))

        "there are no prior OtherIncome in the keystore then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = None))

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url}'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = None))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the same as the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)))

          val goodRequest = callSubmit
          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        def expectedRedirectionForSameInput =
          if (editMode) incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          else incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '$expectedRedirectionForSameInput'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        "the previous OtherIncome entry in keystore is the different from the new input then return a redirect status (SEE_OTHER - 303)" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)))

          val goodRequest = callSubmit
          status(goodRequest) must be(Status.SEE_OTHER)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url}'" in {
          setupMockKeystore(fetchAll = testCacheMapCustom(otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)))

          val goodRequest = callSubmit
          redirectLocation(goodRequest) mustBe Some(incometax.incomesource.controllers.routes.OtherIncomeErrorController.show().url)
          await(goodRequest)
          verifyKeystore(fetchAll = 1, saveOtherIncome = 1)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and saying no to other income" when {

        def callSubmit = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(OtherIncomeForm.option_no)))

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the business journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property 1 page journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_only,
              workForYourself = None,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property 2 pages journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              workForYourself = TestModels.testWorkForYourself_no,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"there are no prior OtherIncome in the keystore then redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the both journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = None
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        def expectedRedirectionForSameInput(noneEditModeUrl: String) =
          if (editMode) incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
          else noneEditModeUrl

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(incometax.business.controllers.routes.BusinessNameController.show().url)
        }' on the business journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(incometax.business.controllers.routes.BusinessNameController.show().url))
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(incometax.subscription.controllers.routes.TermsController.show().url)
        }' on the property journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              workForYourself = TestModels.testWorkForYourself_no,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(incometax.subscription.controllers.routes.TermsController.show().url))

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the same as the new input then redirect to '${
          expectedRedirectionForSameInput(incometax.business.controllers.routes.BusinessNameController.show().url)
        }' on the both journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_no)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(expectedRedirectionForSameInput(incometax.business.controllers.routes.BusinessNameController.show().url))
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the business journey" in {

          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_no_property,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }


        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${incometax.subscription.controllers.routes.TermsController.show().url}' on the property journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_only,
              workForYourself = None,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)

          redirectLocation(goodRequest) mustBe Some(incometax.subscription.controllers.routes.TermsController.show().url)

          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }

        s"the previous OtherIncome entry in keystore is the different from the new input then redirect to '${incometax.business.controllers.routes.BusinessNameController.show().url}' on the both journey" in {
          setupMockKeystore(
            fetchAll = TestModels.testCacheMapCustom(
              rentUkProperty = TestModels.testRentUkProperty_property_and_other,
              workForYourself = TestModels.testWorkForYourself_yes,
              otherIncome = OtherIncomeModel(OtherIncomeForm.option_yes)
            )
          )

          val goodRequest = callSubmit

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest) mustBe Some(incometax.business.controllers.routes.BusinessNameController.show().url)
          await(goodRequest)
          verifyKeystore(saveOtherIncome = 1, fetchAll = 1)
        }
      }

      "Calling the submit action of the OtherIncome controller with an authorised user and with an invalid choice" should {

        val dummy = "Invalid"

        def badrequest = TestOtherIncomeController.submit(isEditMode = editMode)(subscriptionRequest
          .post(OtherIncomeForm.otherIncomeForm, OtherIncomeModel(dummy)))

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

  "The back url not in edit mode" when {
    "the tax year deferral is disabled" should {
      s"point to ${incometax.incomesource.controllers.routes.WorkForYourselfController.show().url} on other income page" in {
        disable(TaxYearDeferralFeature)

        TestOtherIncomeController.backUrl(Both, isEditMode = false) mustBe incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
      }
    }
    "the tax year deferral is enabled and " when {
      "the current date is within the 2017 - 2018 tax year" when {
        "the income source is property" should {
          s"point to ${incometax.incomesource.controllers.routes.CannotReportYetController.show().url} on other income page" in {
            enable(TaxYearDeferralFeature)
            mockGetTaxYearEnd(2018)

            TestOtherIncomeController.backUrl(Property, isEditMode = false) mustBe incometax.incomesource.controllers.routes.CannotReportYetController.show().url
          }
        }

        "the income source is not property" should {
          s"point to ${incometax.incomesource.controllers.routes.WorkForYourselfController.show().url} on other income page" in {
            enable(TaxYearDeferralFeature)
            mockGetTaxYearEnd(2018)

            TestOtherIncomeController.backUrl(Both, isEditMode = false) mustBe incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
          }
        }
      }
      "the current date is after the 2017 - 2018 tax year" should {
        s"point to ${incometax.incomesource.controllers.routes.WorkForYourselfController.show().url} on other income page" in {
          enable(TaxYearDeferralFeature)
          mockGetTaxYearEnd(2019)

          TestOtherIncomeController.backUrl(Both, isEditMode = false) mustBe incometax.incomesource.controllers.routes.WorkForYourselfController.show().url
        }
      }
    }
  }

  "The back url for the old income source mode in edit mode" when {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url} on other income page" in {
      TestOtherIncomeController.backUrl(Property, isEditMode = true) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  "The back url in edit mode" should {
    s"point to ${
      incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    } on other income page" in {
      TestOtherIncomeController.backUrl(Both, isEditMode = true) mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()
}
