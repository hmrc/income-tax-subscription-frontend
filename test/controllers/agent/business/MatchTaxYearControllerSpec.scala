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

import controllers.agent.AgentControllerBaseSpec
import forms.agent.MatchTaxYearForm
import forms.submapping.YesNoMapping
import models.individual.business.MatchTaxYearModel
import models.individual.subscription.IncomeSourceType
import models.{No, Yes}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockKeystoreService

class MatchTaxYearControllerSpec extends AgentControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "MatchTaxYearController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> new Test().controller.show(isEditMode = false),
    "submit" -> new Test().controller.submit(isEditMode = false)
  )

  class Test(fetchMatchTaxYear: Option[MatchTaxYearModel] = None,
             saveMatchTaxYear: Option[MatchTaxYearModel] = None,
             fetchIncomeSource: Option[IncomeSourceType] = None) {

    val controller = new MatchTaxYearController(
      mockAuthService,
      MockKeystoreService
    )

    mockFetchMatchTaxYearFromKeyStore(fetchMatchTaxYear)
    mockFetchIncomeSourceFromKeyStore(fetchIncomeSource)
    setupMockKeystoreSaveFunctions()
  }

  "backUrl" when {
    "in edit mode" should {
      s"return ${controllers.agent.routes.CheckYourAnswersController.show().url}" in new Test {
        controller.backUrl(isEditMode = true) mustBe controllers.agent.routes.CheckYourAnswersController.show().url
      }
    }
    "not in edit mode" when {
      s"return ${controllers.agent.business.routes.BusinessNameController.show().url}" in new Test {
        controller.backUrl(isEditMode = false) mustBe controllers.agent.business.routes.BusinessNameController.show().url
      }
    }
  }

  "submit" when {
    "in edit mode" when {
      "the previous answer matches the current answer" should {
        s"redirect to ${controllers.agent.routes.CheckYourAnswersController.show().url}" in new Test(
          fetchMatchTaxYear = Some(MatchTaxYearModel(Yes)),
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.both))) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.CheckYourAnswersController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }

      s"the answer is changed to '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in new Test(
          fetchMatchTaxYear = Some(MatchTaxYearModel(No)),
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.both))) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingMethodController.show(true).url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }

      s"the answer was changed to '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in new Test(
          fetchMatchTaxYear = Some(MatchTaxYearModel(Yes)),
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.both))) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingPeriodDateController.show(true).url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }
    }

    "not in edit mode" when {
      s"the user answers '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url} when they have selected both income sources" in new Test(
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.both))
        ) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingMethodController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }

        s"redirect to ${routes.WhatYearToSignUpController.show().url} when they have selected only business income sources" in new Test(
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.business))
        ) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.WhatYearToSignUpController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }

      s"the user answers '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url} when they have selected both income sources" in new Test(
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.both))
        ) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingPeriodDateController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }

        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url} when they have selected only business income sources" in new Test(
          fetchIncomeSource = Some(IncomeSourceType(IncomeSourceType.business))
        ) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingPeriodDateController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }

      "the user does not have an income source" should {
        s"redirect to ${controllers.agent.routes.IncomeSourceController.show().url}" in new Test {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, fetchIncomeSource = 1, saveMatchTaxYear = 1)
        }
      }
    }
  }

}
