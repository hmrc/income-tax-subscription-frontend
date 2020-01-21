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
import core.config.featureswitch.{FeatureSwitching, PropertyCashOrAccruals}
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import forms.individual.incomesource.AreYouSelfEmployedForm
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

  override def beforeEach(): Unit = {
    disable(PropertyCashOrAccruals)
    super.beforeEach()
  }

  override val controllerName: String = "AreYouSelfEmployedController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestAreYouSelfEmployedController extends AreYouSelfEmployedController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    MockConfig,
    mockCurrentTimeService
  )

  "Calling the show action of the AreYouSelfEmployed controller with an authorised user" when {

    def call: Future[Result] = TestAreYouSelfEmployedController.show(isEditMode = true)(subscriptionRequest)

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

    def submit(areYouSelfEmployed: AreYouSelfEmployedModel, isEditMode: Boolean): Future[Result] =
      TestAreYouSelfEmployedController.submit(isEditMode = isEditMode)(subscriptionRequest.post(AreYouSelfEmployedForm.areYouSelfEmployedForm, areYouSelfEmployed))

    "invalid submission" should {
      "return bad request (400)" in {
        status(TestAreYouSelfEmployedController.submit(isEditMode = true)(subscriptionRequest.post(AreYouSelfEmployedForm.areYouSelfEmployedForm))) mustBe BAD_REQUEST
      }
    }

    "not in edit mode" when {
      "the user rents out a uk property and is self employed" should {
        s"redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
          setupMockKeystore(fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_property_and_other)))

          val result = submit(testAreYouSelfEmployed_yes, isEditMode = false)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
        }
      }

      "the user rents out a uk property and is not self employed" when {
        "the property cash accruals feature switch is enabled" should {
          s"redirect to ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url}" in {
            enable(PropertyCashOrAccruals)
            setupMockKeystore(fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_property_and_other)))

            val result = submit(testAreYouSelfEmployed_no, isEditMode = false)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)
          }
        }
        "the property cash accruals feature switch is disabled" should {
          s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
            setupMockKeystore(fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_property_and_other)))

            val result = submit(testAreYouSelfEmployed_no, isEditMode = false)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)
          }
        }
      }

      "the user does not rent out a uk property and is self employed" should {
        s"redirect to ${controllers.individual.business.routes.BusinessNameController.show().url}" in {
          setupMockKeystore(fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_no_property)))

          val result = submit(testAreYouSelfEmployed_yes, isEditMode = false)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.business.routes.BusinessNameController.show().url)
        }
      }

      "the user does not rent out a uk property and is not self employed" should {
        s"redirect to ${controllers.individual.incomesource.routes.CannotSignUpController.show().url}" in {
          setupMockKeystore(fetchAll = testCacheMap(rentUkProperty = Some(testRentUkProperty_no_property)))

          val result = submit(testAreYouSelfEmployed_no, isEditMode = false)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.CannotSignUpController.show().url)
        }
      }
    }

    "in edit mode" when {
      "the user keeps their answer the same" should {
        s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          setupMockKeystore(fetchAll = testCacheMap(
            rentUkProperty = Some(testRentUkProperty_no_property),
            areYouSelfEmployed = Some(testAreYouSelfEmployed_yes)
          ))

          val result = submit(testAreYouSelfEmployed_yes, isEditMode = true)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)
        }
      }
      "the user changes their answer" should {
        "redirect to the relevant location" in {
          setupMockKeystore(fetchAll = testCacheMap(
            rentUkProperty = Some(testRentUkProperty_no_property),
            areYouSelfEmployed = Some(testAreYouSelfEmployed_yes)
          ))

          val result = submit(testAreYouSelfEmployed_no, isEditMode = true)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.CannotSignUpController.show().url)
        }
      }
    }

  }
}