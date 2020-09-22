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

package controllers.agent.business

import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import forms.agent.BusinessNameForm
import models.common.BusinessNameModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.BusinessName
import utilities.agent.TestModels._

import scala.concurrent.Future

class BusinessNameControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with FeatureSwitching {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessNameController.show(isEditMode = false),
    "submit" -> TestBusinessNameController.submit(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    mockAuthService,
    MockSubscriptionDetailsService
  )

  "The back url for BusinessNameController" should {

    "return the url for check your answers if in edit mode" in {
      TestBusinessNameController.backUrl(true) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
    }

    "return the url for income source if not in edit mode" in {
      TestBusinessNameController.backUrl(false) mustBe controllers.agent.routes.IncomeSourceController.show().url
    }
  }

  "Calling the showBusinessName action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      mockFetchBusinessNameFromSubscriptionDetails(None)
      mockFetchIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceBusiness))

      status(result) must be(Status.OK)

      await(result)
      verifySubscriptionDetailsSave(BusinessName, 0)
      verifySubscriptionDetailsFetch(BusinessName, 1)

    }
  }

  "Calling the submitBusinessName action of the BusinessNameController with an authorised user and valid submission" should {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    def callSubmit(isEditMode: Boolean, businessNameModel: BusinessNameModel): Future[Result] = {
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(BusinessNameForm.businessNameForm.form, businessNameModel)
      )
    }

    "When is not in edit mode" when {
      "the user is business only" should {
        s"redirect to ${controllers.agent.business.routes.WhatYearToSignUpController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.WhatYearToSignUpController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(BusinessName, 1)
        }
      }

      "the user is business and income source" should {
        s"redirect to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url}" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusinessAndUkProperty))

          val goodRequest = callShow(isEditMode = false)

          status(goodRequest) mustBe Status.SEE_OTHER
          redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.BusinessAccountingMethodController.show().url)

          await(goodRequest)
          verifySubscriptionDetailsFetchAll(2)
          verifySubscriptionDetailsSave(BusinessName, 1)
        }
      }
    }

    "the income source is false" should {
      s"redirect to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceNone))

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(2)
        verifySubscriptionDetailsSave(BusinessName, 1)
      }
    }

    "there is no income source" should {
      s"redirect to ${controllers.agent.routes.IncomeSourceController.show().url}" in {
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(2)
        verifySubscriptionDetailsSave(BusinessName, 1)
      }
    }

    "When is in edit mode" should {
      s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show()}" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness, businessName = testBusinessName))

        val goodRequest = callSubmit(isEditMode = true, testBusinessName)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetch(BusinessName, 2)
        verifySubscriptionDetailsSave(BusinessName, 1)
      }
    }
  }
    "Calling the submitBusinessName action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      mockFetchIncomeSourceFromSubscriptionDetails(Some(testIncomeSourceBusiness))

      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
    }

    "in edit mode" should {
      s"point to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessNameController.backUrl(
          isEditMode = true
        ) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }
  }

  authorisationTests()

}
