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

import core.config.featureswitch.{FeatureSwitching, NewIncomeSourceFlowFeature, TaxYearDeferralFeature}
import core.controllers.ControllerBaseSpec
import core.services.mocks.MockKeystoreService
import core.utils.TestModels._
import incometax.incomesource.forms.WorkForYourselfForm
import incometax.incomesource.models.WorkForYourselfModel
import incometax.incomesource.services.mocks.MockCurrentTimeService
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{await, status, _}
import uk.gov.hmrc.http.NotFoundException

class WorkForYourselfControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching
  with MockCurrentTimeService {

  override val controllerName: String = "WorkForYourselfController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestWorkForYourselfController extends WorkForYourselfController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService,
    mockCurrentTimeService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(NewIncomeSourceFlowFeature)
    disable(TaxYearDeferralFeature)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    disable(NewIncomeSourceFlowFeature)
    disable(TaxYearDeferralFeature)
  }

  "Calling the show action of the WorkForYourself controller with an authorised user" when {

    def call = TestWorkForYourselfController.show(isEditMode = true)(subscriptionRequest)

    "the new income source flow feature is disabled" should {
      "return not found (404)" in {
        disable(NewIncomeSourceFlowFeature)
        intercept[NotFoundException](await(call))
      }
    }

    "the new income source flow feature is enabled" when {
      "There are no rentUkProperty data" should {
        "return ok (200)" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll = None)

          val result = call
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.RentUkPropertyController.show().url

          await(result)
          verifyKeystore(fetchAll = 1, saveWorkForYourself = 0)
        }
      }
      "There are no rentUkProperty in keystore and but it doesn't needs work for yourself to be answered" should {
        "return ok (200)" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testNewIncomeSourceProperty_1page.rentUkProperty,
              workForYourself = testNewIncomeSourceProperty_1page.workForYourself)
          )

          val result = call
          status(result) must be(Status.SEE_OTHER)
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.RentUkPropertyController.show().url

          await(result)
          verifyKeystore(fetchAll = 1, saveWorkForYourself = 0)
        }
      }
      "There is rentUkProperty in keystore and it needs work for yourself answered" should {
        "return ok (200)" in {
          enable(NewIncomeSourceFlowFeature)
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testNewIncomeSourceBoth.rentUkProperty,
              workForYourself = testNewIncomeSourceBoth.workForYourself)
          )

          val result = call
          status(result) must be(Status.OK)

          await(result)
          verifyKeystore(fetchAll = 1, saveWorkForYourself = 0)
        }
      }
    }
  }

  "Calling the submit action of the WorkForYourself controller with an authorised user" when {
    def call(workForYourself: WorkForYourselfModel, isEditMode: Boolean) =
      TestWorkForYourselfController.submit(isEditMode = true)(subscriptionRequest.post(WorkForYourselfForm.workForYourselfForm, workForYourself))

    "the new income source flow feature is disabled" should {
      "return not found (404)" in {
        disable(NewIncomeSourceFlowFeature)
        intercept[NotFoundException](await(call(testWorkForYourself_yes, isEditMode = false)))
      }
    }

    "invalid submission" should {
      "return bad request (400)" in {
        status(TestWorkForYourselfController.submit(isEditMode = true)(subscriptionRequest.post(WorkForYourselfForm.workForYourselfForm))) mustBe BAD_REQUEST
      }
    }

    "not in editMode" when {
      def submit(workForYourself: WorkForYourselfModel = testWorkForYourself_yes) = call(workForYourself, isEditMode = false)


      "a property only submission" when {
        "TaxYearDeferralFeature is disabled" should {
          "redirect to correctly" in {
            setupMockKeystore(fetchAll =
              testCacheMapCustom(
                rentUkProperty = testNewIncomeSourceProperty_2page.rentUkProperty
              )
            )
            val result = submit(testNewIncomeSourceProperty_2page.workForYourself.get)
            status(result) mustBe SEE_OTHER
            redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
          }
        }
        "TaxYearDeferralFeature is enabled" when {
          "we're in the 2017 - 2018 tax year" should {
            "redirect to correctly" in {
              enable(TaxYearDeferralFeature)
              mockGetTaxYearEnd(2018)
              setupMockKeystore(fetchAll =
                testCacheMapCustom(
                  rentUkProperty = testNewIncomeSourceProperty_2page.rentUkProperty
                )
              )
              val result = submit(testNewIncomeSourceProperty_2page.workForYourself.get)
              status(result) mustBe SEE_OTHER
              redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.CannotReportYetController.show().url
            }
          }
          "we're after the 2017 - 2018 tax year" should {
            "redirect to correctly" in {
              enable(TaxYearDeferralFeature)
              mockGetTaxYearEnd(2019)
              setupMockKeystore(fetchAll =
                testCacheMapCustom(
                  rentUkProperty = testNewIncomeSourceProperty_2page.rentUkProperty
                )
              )
              val result = submit(testNewIncomeSourceProperty_2page.workForYourself.get)
              status(result) mustBe SEE_OTHER
              redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
            }
          }
        }
      }

      "a business only submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testNewIncomeSourceBusiness.rentUkProperty
            )
          )
          val result = submit(testNewIncomeSourceBusiness.workForYourself.get)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
        }
      }

      "a business and property submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testNewIncomeSourceBoth.rentUkProperty
            )
          )
          val result = submit(testNewIncomeSourceBoth.workForYourself.get
          )
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.OtherIncomeController.show().url
        }
      }

      "user not qualified submission" should {
        "redirect to correctly" in {
          setupMockKeystore(fetchAll =
            testCacheMapCustom(
              rentUkProperty = testNewIncomeSourceNotQualified.rentUkProperty
            )
          )
          val result = submit(testNewIncomeSourceNotQualified.workForYourself.get)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe incometax.incomesource.controllers.routes.CannotSignUpController.show().url
        }
      }
    }
  }

  // TOOD tests for edit mode...
}
