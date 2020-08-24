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

import config.MockConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import forms.individual.incomesource.IncomeSourceForm
import models.individual.incomesource.IncomeSourceModel
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.IndividualIncomeSource

import scala.concurrent.Future

class IncomeSourceControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockConfig
  with FeatureSwitching {

  class TestIncomeSourceController extends IncomeSourceController(
    mockAuthService,
    MockSubscriptionDetailsService
  )

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(ForeignProperty)
    super.beforeEach()
  }

  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> new TestIncomeSourceController().show(isEditMode = true),
    "submit" -> new TestIncomeSourceController().submit(isEditMode = true)
  )

  "test" should {
    "en" in {
      val m: Messages = messagesApi.preferred(subscriptionRequest)
      m must not be None
      m.apply("base.back") must be("Back")
    }
  }

  "Calling the show action of the IncomeSourceController with an authorised user" when {

    def call: Future[Result] = new TestIncomeSourceController().show(isEditMode = true)(subscriptionRequest)

    "the new income source flow" should {
      "return ok (200)" in {
        mockFetchIndividualIncomeSourceFromSubscriptionDetails(None)

        val result = call
        status(result) must be(Status.OK)

        await(result)
        verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
        verifySubscriptionDetailsSave(IndividualIncomeSource, 0)
      }
    }

  }

  "Calling the submit action of the IncomeSource controller with an authorised user and valid submission" should {

    def callSubmit(incomeSourceModel: IncomeSourceModel,
                   isEditMode: Boolean
                  ): Future[Result] = {
      new TestIncomeSourceController().submit(isEditMode = isEditMode)(
        subscriptionRequest.post(IncomeSourceForm.incomeSourceForm, incomeSourceModel)
      )
    }

    "When it is not edit mode" should {
      "self-employed is checked and rent UK property and foreign property are NOT checked" when {
        "redirect to BusinessName page" in {
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(IncomeSourceModel(true, false, false), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.business.routes.BusinessNameController.show().url
          await(goodRequest)
          verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
          verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
        }
      }

      "Rent UK property is checked and self-employed, foreign property are NOT checked" should {
        "Release Four feature switch is disabled" when {
          "redirect to the PropertyAccounting method page" in {
            setupMockSubscriptionDetailsSaveFunctions()
            val goodRequest = callSubmit(IncomeSourceModel(false, true, false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
            verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
          }
        }
        "Release Four feature switch is enabled" when {
          "redirect to the PropertyCommencementDate page" in {
            enable(ReleaseFour)
            setupMockSubscriptionDetailsSaveFunctions()
            val goodRequest = callSubmit(IncomeSourceModel(false, true, false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.PropertyCommencementDateController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
            verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
          }
        }
      }

      "Foreign property is checked and self-employed, UK property are NOT checked" should {
        "Release Four feature switch is disabled" when {
          "redirect to the PropertyAccounting method page" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(IncomeSourceModel(false, false, true), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
            verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
          }
        }
        "Release Four feature switch is enabled" when {
          "redirect to the PropertyAccounting method page" in {
            enable(ForeignProperty)
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(IncomeSourceModel(false, false, true), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.individual.business.routes.OverseasPropertyCommencementDateController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
            verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
          }
        }
      }

      "All self-employed, rent UK property and foreign property are checked" when {
        "redirect to BusinessName page" in {
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(IncomeSourceModel(true, true, true), isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get must be(controllers.individual.business.routes.BusinessNameController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
          verifySubscriptionDetailsSave(IndividualIncomeSource, 1)
        }
      }

      "When it is in edit mode and user's selection has not changed" should {
        s"return an SEE OTHER (303) when self-employed is checked and rent uk property and foreign property are NOT checked" +
          s"${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
          mockFetchIndividualIncomeSourceFromSubscriptionDetails(IncomeSourceModel(true, false, false))


          val goodRequest = callSubmit(IncomeSourceModel(true, false, false), isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IndividualIncomeSource, 1)
          verifySubscriptionDetailsSave(IndividualIncomeSource, 0)
        }
      }


      "Calling the submit action of the IncomeSource controller with an authorised user and invalid submission" should {
        lazy val badRequest = new TestIncomeSourceController().submit(isEditMode = true)(subscriptionRequest)

        "return a bad request status (400)" in {
          status(badRequest) must be(Status.BAD_REQUEST)

          await(badRequest)
          verifySubscriptionDetailsFetch(IndividualIncomeSource, 0)
          verifySubscriptionDetailsSave(IndividualIncomeSource, 0)
        }
      }


      "The back url" should {
        s"point to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url} on income source page" in {
          new TestIncomeSourceController().backUrl mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
        }
      }

      authorisationTests()

    }
  }
}
