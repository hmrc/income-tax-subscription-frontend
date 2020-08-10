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

import config.featureswitch.FeatureSwitching
import forms.agent.IncomeSourceForm
import models.individual.subscription.{Both, Business, IncomeSourceType, Property}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.IncomeSource
import utilities.agent.TestModels

import scala.concurrent.Future

class IncomeSourceControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with FeatureSwitching {

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestIncomeSourceController.show(isEditMode = true),
    "submit" -> TestIncomeSourceController.submit(isEditMode = true)
  )

  object TestIncomeSourceController extends IncomeSourceController(
    mockAuthService,
    MockSubscriptionDetailsService
  )(executionContext, appConfig, mockMessagesControllerComponents)

  "Calling the showIncomeSource action of the IncomeSource controller with an authorised user" should {

    lazy val result = TestIncomeSourceController.show(isEditMode = true)(subscriptionRequest)

    "return ok (200)" in {
      mockFetchIncomeSourceFromSubscriptionDetails(None)

      status(result) must be(Status.OK)

      await(result)
      verifySubscriptionDetailsFetch(IncomeSource, 1)
      verifySubscriptionDetailsSave(IncomeSource, 0)
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and valid submission" when {

    def callSubmit(option: IncomeSourceType, isEditMode: Boolean): Future[Result] = TestIncomeSourceController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(IncomeSourceForm.incomeSourceForm, option)
    )

    "the eligibility pages feature switch is enabled" should {
      s"redirect to ${business.routes.MatchTaxYearController.show().url}" when {
        "not in edit mode" when {
          "the income source is business" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val result = await(callSubmit(Business, isEditMode = false))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(business.routes.BusinessNameController.show().url)

            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
          "the income source is both" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val result = await(callSubmit(Both, isEditMode = false))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(business.routes.BusinessNameController.show().url)

            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
        "in edit mode" when {
          "income source has changed" in {
            setupMockSubscriptionDetailsSaveFunctions()
            mockFetchIncomeSourceFromSubscriptionDetails(Business)

            val result = await(callSubmit(Both, isEditMode = true))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(business.routes.BusinessNameController.show().url)

            verifySubscriptionDetailsFetch(IncomeSource, 2)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }
      }
      s"redirect to ${business.routes.PropertyAccountingMethodController.show().url}" when {
        "not in edit mode" when {
          "income source is property" in {

            setupMockSubscriptionDetailsSaveFunctions()

            val result = await(callSubmit(Property, isEditMode = false))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(business.routes.PropertyAccountingMethodController.show().url)
          }
        }
        "in edit mode" when {
          "income source has changed to property" in {

            setupMockSubscriptionDetailsSaveFunctions()
            mockFetchIncomeSourceFromSubscriptionDetails(Business)

            val result = await(callSubmit(Property, isEditMode = true))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(business.routes.PropertyAccountingMethodController.show().url)
          }
        }

      }
    }

    "the eligibility pages feature switch is disabled" when {

      "it is not edit mode" when {
        "the income source is business" should {
          s"return a SEE_OTHER with a redirect location of ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
            setupMockSubscriptionDetailsSaveFunctions()

            val goodRequest = callSubmit(Both, isEditMode = false)

            status(goodRequest) must be(Status.SEE_OTHER)
            redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.BusinessNameController.show().url

            await(goodRequest)
            verifySubscriptionDetailsFetch(IncomeSource, 1)
            verifySubscriptionDetailsSave(IncomeSource, 1)
          }
        }

        s"return a SEE OTHER (303) for both and goto ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(Both, isEditMode = false)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.BusinessNameController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }

      "it is in edit mode and user's selection has not changed" should {
        s"return an SEE OTHER (303) for business and goto ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceBusiness)

          val goodRequest = callSubmit(Business, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 0)
        }

        s"return a SEE OTHER (303) for property and goto ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceProperty)

          val goodRequest = callSubmit(Property, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 0)
        }

        s"return a SEE OTHER (303) for both and goto ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceBoth)

          val goodRequest = callSubmit(Both, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.routes.CheckYourAnswersController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 1)
          verifySubscriptionDetailsSave(IncomeSource, 0)
        }
      }

      "it is in edit mode and user's selection has changed" should {
        s"return an SEE OTHER (303) for business and goto ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceBoth)
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(Business, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.BusinessNameController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 2)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }

        s"return a SEE OTHER (303) for property and goto ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceBoth)
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(Property, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.PropertyAccountingMethodController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 2)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }

        s"return a SEE OTHER (303) for both and goto ${controllers.agent.business.routes.BusinessNameController.show().url}" in {
          mockFetchIncomeSourceFromSubscriptionDetails(TestModels.testIncomeSourceBusiness)
          setupMockSubscriptionDetailsSaveFunctions()

          val goodRequest = callSubmit(Both, isEditMode = true)

          status(goodRequest) must be(Status.SEE_OTHER)
          redirectLocation(goodRequest).get mustBe controllers.agent.business.routes.BusinessNameController.show().url

          await(goodRequest)
          verifySubscriptionDetailsFetch(IncomeSource, 2)
          verifySubscriptionDetailsSave(IncomeSource, 1)
        }
      }
    }
  }

  "Calling the submitIncomeSource action of the IncomeSource controller with an authorised user and invalid submission" should {
    lazy val badRequest = TestIncomeSourceController.submit(isEditMode = true)(subscriptionRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifySubscriptionDetailsFetch(IncomeSource, 0)
      verifySubscriptionDetailsSave(IncomeSource, 0)
    }
  }

  "The back url" should {
    s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url} on income source page" in {
      TestIncomeSourceController.backUrl mustBe controllers.agent.routes.CheckYourAnswersController.show().url
    }
  }

  authorisationTests()

}
