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

package controllers.agent

import config.MockConfig
import config.featureswitch.FeatureSwitch.{ForeignProperty, ReleaseFour}
import config.featureswitch.FeatureSwitching
import forms.agent.IncomeSourceForm
import models.common.IncomeSourceModel
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.IncomeSource
import scala.concurrent.Future

class IncomeSourceControllerSpec extends AgentControllerBaseSpec
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
        mockFetchIncomeSourceFromSubscriptionDetails(None)

        val result = call
        status(result) must be(Status.OK)

        await(result)
        verifySubscriptionDetailsFetch(IncomeSource, 1)
        verifySubscriptionDetailsSave(IncomeSource, 0)
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
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.BusinessNameController.show().url
          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "Rent UK property is checked and self-employed, foreign property are NOT checked" should {
        "Release Four feature switch is disabled" when {
          "redirect to the PropertyAccounting method page" in {
            setupMockSubscriptionDetailsSaveFunctions()
            val goodRequest = callSubmit(IncomeSourceModel(false, true, false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.agent.business.routes.PropertyAccountingMethodController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "Self-employed, rent UK property are checked" when {
          "redirect to BusinessName page" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(IncomeSourceModel(true, true, false), isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get must be(controllers.agent.business.routes.BusinessNameController.show().url)

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        "When it is in edit mode and user's selection has not changed" should {
          s"return an SEE OTHER (303) when self-employed is checked and rent uk property and foreign property are NOT checked" +
            s"${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
            mockFetchIncomeSourceFromSubscriptionDetails(IncomeSourceModel(true, false, false))


            val goodRequest = callSubmit(IncomeSourceModel(true, false, false), isEditMode = true)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 0)
          }
        }


        "Calling the submit action of the IncomeSource controller with an authorised user and invalid submission" should {
          lazy val badRequest = new TestIncomeSourceController().submit(isEditMode = true)(subscriptionRequest)

          "return a bad request status (400)" in {
            status(badRequest) must be(Status.BAD_REQUEST)

            await(badRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 0)
            verifySubscriptionDetailsSave(IncomeSource, 0)
          }
        }


        "The back url" should {
          s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url} on income source page" in {
            new TestIncomeSourceController().backUrl mustBe controllers.agent.routes.CheckYourAnswersController.show().url
          }
        }

        authorisationTests()

      }
    }
  }
}
