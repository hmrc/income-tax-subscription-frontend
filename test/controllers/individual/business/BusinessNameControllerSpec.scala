/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import controllers.ControllerBaseSpec
import forms.individual.business.BusinessNameForm
import models.common.business.BusinessNameModel
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.BusinessName
import utilities.TestModels._

import scala.concurrent.Future

class BusinessNameControllerSpec extends ControllerBaseSpec with MockSubscriptionDetailsService with MockAuditingService {

  override val controllerName: String = "BusinessNameController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestBusinessNameController.show(isEditMode = false),
    "submit" -> TestBusinessNameController.submit(isEditMode = false)
  )

  object TestBusinessNameController extends BusinessNameController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService
  )

  "Calling the show action of the BusinessNameController with an authorised user" should {

    lazy val result = TestBusinessNameController.show(isEditMode = false)(subscriptionRequest)

    "return ok (200)" in {
      mockFetchBusinessNameFromSubscriptionDetails(None)

      status(result) must be(Status.OK)

      await(result)
      verifySubscriptionDetailsFetch(BusinessName, 1)
      verifySubscriptionDetailsSave(BusinessName, 0)

    }
  }

  "Calling the submit action of the BusinessNameController with an authorised user on the sign up journey and valid submission" when {

    def callShow(isEditMode: Boolean): Future[Result] =
      TestBusinessNameController.submit(isEditMode = isEditMode)(
        subscriptionRequest
          .post(BusinessNameForm.businessNameForm.form, BusinessNameModel("Test business"))
      )

    "it is in edit mode" should {
      s"return a redirect status (SEE_OTHER - 303) to '${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchBusinessNameFromSubscriptionDetails(None)

        val goodRequest = callShow(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.subscription.routes.CheckYourAnswersController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetch(BusinessName, 1)
        verifySubscriptionDetailsSave(BusinessName, 1)
      }
    }

    "it is not in edit mode" when {
      s"redirect to ${controllers.individual.business.routes.BusinessAccountingMethodController.show().url}" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap(incomeSource = testIncomeSourceBusiness))

        val goodRequest = callShow(isEditMode = false)

        status(goodRequest) mustBe Status.SEE_OTHER
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.BusinessAccountingMethodController.show().url)

        await(goodRequest)
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(BusinessName, 1)
      }
    }

  }

  "Calling the submit action of the BusinessNameController with an authorised user and invalid submission" should {
    lazy val badRequest = TestBusinessNameController.submit(isEditMode = false)(subscriptionRequest)

    "return a bad request status (400)" in {
      status(badRequest) must be(Status.BAD_REQUEST)

      await(badRequest)
      verifySubscriptionDetailsFetch(BusinessName, 0)
      verifySubscriptionDetailsSave(BusinessName, 0)
    }
  }

  "The back url" when {
    "in edit mode" should {
      s"redirect to ${controllers.individual.subscription.routes.CheckYourAnswersController.show().url}" in {
        TestBusinessNameController.backUrl(isEditMode = true) mustBe controllers.individual.subscription.routes.CheckYourAnswersController.show().url
      }
    }
    "not in edit mode" should {
      s"redirect to ${controllers.individual.incomesource.routes.IncomeSourceController.show().url}" in {
        TestBusinessNameController.backUrl(isEditMode = false) mustBe controllers.individual.incomesource.routes.IncomeSourceController.show().url
      }
    }
  }

  authorisationTests()

}
