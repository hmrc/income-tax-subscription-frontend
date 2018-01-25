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
import core.utils.TestModels
import incometax.incomesource.forms.RentUkPropertyForm
import incometax.incomesource.models.RentUkPropertyModel
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import uk.gov.hmrc.http.NotFoundException

class RentUkPropertyControllerSpec extends ControllerBaseSpec
  with MockKeystoreService
  with FeatureSwitching {

  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRentUkPropertyController.show(isEditMode = true),
    "submit" -> TestRentUkPropertyController.submit(isEditMode = true)
  )

  object TestRentUkPropertyController extends RentUkPropertyController(
    MockBaseControllerConfig,
    messagesApi,
    MockKeystoreService,
    mockAuthService
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
  }

  override def afterEach(): Unit = {
    super.beforeEach()
    disable(TaxYearDeferralFeature)
  }

  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(subscriptionRequest)
      m must not be null
      m.apply("base.back") must be("Back")
    }
  }

  "Calling the show action of the RentUkPropertyController with an authorised user" when {

    def call = TestRentUkPropertyController.show(isEditMode = true)(subscriptionRequest)

    "the new income source flow feature is enabled" should {
      "return ok (200)" in {
        enable(NewIncomeSourceFlowFeature)
        setupMockKeystore(fetchRentUkProperty = None)

        val result = call
        status(result) must be(Status.OK)

        await(result)
        verifyKeystore(fetchRentUkProperty = 1, saveRentUkProperty = 0)
      }
    }

    "the new income source flow feature is disabled" should {
      "return not_found (404)" in {
        disable(NewIncomeSourceFlowFeature)
        intercept[NotFoundException](await(call))
      }
    }
  }

  "Calling the submit action of the RentUkProperty controller with an authorised user and valid submission" should {


    def callSubmit(option: (String, Option[String]), isEditMode: Boolean) = TestRentUkPropertyController.submit(isEditMode = isEditMode)(subscriptionRequest
      .post(RentUkPropertyForm.rentUkPropertyForm, RentUkPropertyModel(option._1, option._2)))

    "When it is not edit mode" should {
      s"return a NOT_IMPLEMENTED (501) when answering 'NO' to rent uk property" in {

        enable(NewIncomeSourceFlowFeature)

        setupMockKeystoreSaveFunctions()

        val goodRequest = callSubmit((RentUkPropertyForm.option_no, None), isEditMode = false)

        status(goodRequest) must be(Status.NOT_IMPLEMENTED)

        await(goodRequest)
        verifyKeystore(fetchRentUkProperty = 0, saveRentUkProperty = 1)
      }

      s"return a NOT_IMPLEMENTED (501) when answering 'Yes' to rent uk property and then 'No' to only income source" in {

        enable(NewIncomeSourceFlowFeature)

        setupMockKeystoreSaveFunctions()

        val goodRequest = callSubmit((RentUkPropertyForm.option_yes, RentUkPropertyForm.option_no), isEditMode = false)

        status(goodRequest) must be(Status.NOT_IMPLEMENTED)

        await(goodRequest)
        verifyKeystore(fetchRentUkProperty = 0, saveRentUkProperty = 1)
      }

      s"return a SEE_OTHER (303) when answering 'Yes' to rent uk property and then 'Yes' to only income source" in {
        enable(NewIncomeSourceFlowFeature)

        setupMockKeystoreSaveFunctions()

        val goodRequest = callSubmit((RentUkPropertyForm.option_yes, RentUkPropertyForm.option_yes), isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get must be(incometax.incomesource.controllers.routes.OtherIncomeController.show().url)

        await(goodRequest)
        verifyKeystore(fetchRentUkProperty = 0, saveRentUkProperty = 1)
      }
    }


    "When it is in edit mode and user's selection has not changed" should {
      s"return an SEE OTHER (303) for 'No' to rent a uk property and goto " +
        s"${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_no, None))

        val goodRequest = callSubmit((RentUkPropertyForm.option_no, None), isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest).get mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url

        await(goodRequest)
        verifyKeystore(fetchRentUkProperty = 1, saveRentUkProperty = 1)
      }
    }

    "When it is in edit mode and user's selection has changed" should {
      s"return an SEE OTHER (303) and goto ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url}" in {
        setupMockKeystore(fetchRentUkProperty = RentUkPropertyModel(RentUkPropertyForm.option_no, None))

        val goodRequest = callSubmit((RentUkPropertyForm.option_yes, RentUkPropertyForm.option_no), isEditMode = true)

        status(goodRequest) must be(Status.NOT_IMPLEMENTED)

        await(goodRequest)
        verifyKeystore(fetchRentUkProperty = 1, saveRentUkProperty = 1)
      }
    }
  }

  "Calling the submit action of the RentUkProperty controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestRentUkPropertyController.submit(isEditMode = true)(subscriptionRequest)

    "return a bad request status (400)" in {
      enable(NewIncomeSourceFlowFeature)
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifyKeystore(fetchRentUkProperty = 0, saveRentUkProperty = 0)
    }
  }

  "The back url" should {
    s"point to ${incometax.subscription.controllers.routes.CheckYourAnswersController.show().url} on rent uk property page" in {
      TestRentUkPropertyController.backUrl mustBe incometax.subscription.controllers.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
