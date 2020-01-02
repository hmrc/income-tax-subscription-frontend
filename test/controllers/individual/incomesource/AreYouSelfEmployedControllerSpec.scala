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
import core.config.MockConfig
import core.config.featureswitch.FeatureSwitching
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import incometax.incomesource.forms.AreYouSelfEmployedForm
import incometax.incomesource.models.AreYouSelfEmployedModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, status, _}

import scala.concurrent.Future

class AreYouSelfEmployedControllerSpec extends ControllerBaseSpec

  with MockKeystoreService
  with FeatureSwitching
  with MockConfig
  with MockCurrentTimeService {

  override val controllerName: String = "AreYouSelfEmployedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestAreYouSelfEmployedController$$ extends AreYouSelfEmployedController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    MockConfig,
    mockCurrentTimeService
  )

  "Calling the show action of the AreYouSelfEmployed controller with an authorised user" when {

    def call = TestAreYouSelfEmployedController$$.show(isEditMode = true)(subscriptionRequest)

    "There are no rentUkProperty data" should {
      "return ok (200)" in {
        setupMockKeystore(fetchAll = None)

        val result = call
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get mustBe controllers.individual.incomesource.routes.RentUkPropertyController.show().url

        await(result)
        verifyKeystore(fetchAll = 1, saveAreYouSelfEmployed = 0)
      }

      "There are no rentUkProperty in keystore and but it doesn't needs are you self-employed to be answered" should {
        "return ok (200)" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_only,
              areYouSelfEmployed = None)
          )

          val result = call
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.RentUkPropertyController.show().url

          await(result)
          verifyKeystore(fetchAll = 1, saveAreYouSelfEmployed = 0)
        }
      }
      "There is rentUkProperty in keystore and it needs are you self-employed answered" should {
        "return ok (200)" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              areYouSelfEmployed = testAreYouSelfEmployed_yes)
          )

          val result = call
          status(result) must be(Status.OK)

          await(result)
          verifyKeystore(fetchAll = 1, saveAreYouSelfEmployed = 0)
        }
      }
    }
  }

  "Calling the submit action of the AreYouSelfEmployed controller with an authorised user" when {
    def call(areYouSelfEmployed: AreYouSelfEmployedModel, isEditMode: Boolean) =
      TestAreYouSelfEmployedController$$.submit(isEditMode = isEditMode)(subscriptionRequest.post(AreYouSelfEmployedForm.areYouSelfEmployedForm, areYouSelfEmployed))

    "invalid submission" should {
      "return bad request (400)" in {
        status(TestAreYouSelfEmployedController$$.submit(isEditMode = true)(subscriptionRequest.post(AreYouSelfEmployedForm.areYouSelfEmployedForm))) mustBe BAD_REQUEST
      }
    }

    "not in editMode" when {
      def submit(areYouSelfEmployed: AreYouSelfEmployedModel = testAreYouSelfEmployed_yes) = call(areYouSelfEmployed, isEditMode = false)


      "a property only submission" when {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other
            )
          )
          val result = submit(testAreYouSelfEmployed_no)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
        }
      }

      "a business only submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property
            )
          )
          val result = submit(testAreYouSelfEmployed_yes)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
        }
      }

      "a business and property submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other
            )
          )
          val result = submit(testAreYouSelfEmployed_yes)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
        }
      }

      "user not qualified submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property
            )
          )
          val result = submit(testAreYouSelfEmployed_no)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.CannotSignUpController.show().url
        }
      }
    }


    "in editMode" when {
      def submit(areYouSelfEmployed: AreYouSelfEmployedModel = testAreYouSelfEmployed_yes) = call(areYouSelfEmployed, isEditMode = true)

      "the user kept their answer the same" should {
        "return to check your answers" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other,
              areYouSelfEmployed = testAreYouSelfEmployed_no
            )
          )
          val result = submit(testAreYouSelfEmployed_no)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }

      "the user changed their answer and" when {
        "a change to property only submission and" when {
          "redirect to correctly" in {
            setupMockKeystore(fetchAll =
              testCacheMapCustom(
                rentUkProperty = testRentUkProperty_property_and_other,
                areYouSelfEmployed = testAreYouSelfEmployed_yes
              )
            )
            val result = submit(testAreYouSelfEmployed_no)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
          }
        }

        "a change to business only submission" should {
          "redirect to correctly" in {
            setupMockKeystore(fetchAll =
              testCacheMapCustom(
                rentUkProperty = testRentUkProperty_property_and_other,
                areYouSelfEmployed = testAreYouSelfEmployed_no
              )
            )
            val result = submit(testAreYouSelfEmployed_yes)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
          }
        }

        "a change to business and property submission" should {
          "redirect to correctly" in {
            setupMockKeystore(fetchAll =
              testCacheMapCustom(
                rentUkProperty = testRentUkProperty_property_and_other,
                areYouSelfEmployed = testAreYouSelfEmployed_no
              )
            )
            val result = submit(testAreYouSelfEmployed_yes)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe controllers.individual.incomesource.routes.OtherIncomeController.show().url
          }
        }

      }
    }

    "the eligibility pages feature switch is enabled" should {

      object TestAreYouSelfEmployedController extends AreYouSelfEmployedController(
        MockBaseControllerConfig,
        messagesApi,
        MockKeystoreService,
        mockAuthService,
        new MockConfig {
          override val eligibilityPagesEnabled: Boolean = true
        },
        mockCurrentTimeService
      )

      def call(areYouSelfEmployed: AreYouSelfEmployedModel, isEditMode: Boolean): Future[Result] =
        TestAreYouSelfEmployedController.submit(isEditMode = isEditMode)(subscriptionRequest.post(AreYouSelfEmployedForm.areYouSelfEmployedForm, areYouSelfEmployed))

      def submit(areYouSelfEmployed: AreYouSelfEmployedModel, isEditMode: Boolean = false): Future[Result] = call(areYouSelfEmployed, isEditMode = isEditMode)


      "redirect to the check your answers page" when {
        "the user has chosen property only" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other
            )
          )
          val result = submit(testAreYouSelfEmployed_no)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }

        "the user has chosen business only" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property
            )
          )
          val result = submit(testAreYouSelfEmployed_yes)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.business.routes.BusinessNameController.show().url
        }

        "the user has chosen business and property" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_property_and_other
            )
          )
          val result = submit(testAreYouSelfEmployed_yes)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.business.routes.BusinessNameController.show().url
        }

        "the user does not qualify" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testRentUkProperty_no_property
            )
          )
          val result = submit(testAreYouSelfEmployed_no)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.individual.incomesource.routes.CannotSignUpController.show().url
        }
      }
    }
  }

}
