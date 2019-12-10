/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.controllers.business

import agent.controllers.AgentControllerBaseSpec
import agent.forms.MatchTaxYearForm
import agent.services.mocks.MockKeystoreService
import core.forms.submapping.YesNoMapping
import core.models.{No, Yes}
import incometax.business.models.MatchTaxYearModel
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._

class MatchTaxYearControllerSpec extends AgentControllerBaseSpec with MockKeystoreService {

  override val controllerName: String = "MatchTaxYearController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> new Test().controller.show(isEditMode = false),
    "submit" -> new Test().controller.submit(isEditMode = false)
  )

  class Test(fetchMatchTaxYear: Option[MatchTaxYearModel] = None, saveMatchTaxYear: Option[MatchTaxYearModel] = None) {

    val controller = new MatchTaxYearController(
      MockBaseControllerConfig,
      messagesApi,
      MockKeystoreService,
      mockAuthService
    )

    setupMockKeystore(fetchMatchTaxYear = fetchMatchTaxYear)

  }

  "backUrl" when {
    "in edit mode" should {
      s"return ${agent.controllers.routes.CheckYourAnswersController.show().url}" in new Test {
        controller.backUrl(isEditMode = true) mustBe agent.controllers.routes.CheckYourAnswersController.show().url
      }
    }
    "not in edit mode" should {
      s"return ${agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url}" in new Test {
        controller.backUrl(isEditMode = false) mustBe agent.controllers.business.routes.BusinessAccountingPeriodPriorController.show().url
      }
    }
  }

  "show" when {
    "no previous answer is in keystore" should {
      s"return $OK" in new Test {
        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifyKeystore(fetchMatchTaxYear = 1)
      }
    }
    "a previous answer is in keystore" should {
      s"return $OK" in new Test(fetchMatchTaxYear = Some(MatchTaxYearModel(Yes))) {
        val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

        status(result) mustBe OK

        verifyKeystore(fetchMatchTaxYear = 1)
      }
    }
  }

  "submit" when {
    "in edit mode" when {
      "the previous answer matches the current answer" should {
        s"redirect to ${agent.controllers.routes.CheckYourAnswersController.show().url}" in new Test(fetchMatchTaxYear = Some(MatchTaxYearModel(Yes))) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(agent.controllers.routes.CheckYourAnswersController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, saveMatchTaxYear = 1)
        }
      }
      s"the answer is changed to '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in new Test(fetchMatchTaxYear = Some(MatchTaxYearModel(No))) {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingMethodController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, saveMatchTaxYear = 1)
        }
      }
      s"the answer was changed to '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in new Test {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = true)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingPeriodDateController.show().url)
        }
      }
    }
    "not in edit mode" when {
      s"the user answers '$Yes'" should {
        s"redirect to ${routes.BusinessAccountingMethodController.show().url}" in new Test {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_yes)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingMethodController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, saveMatchTaxYear = 1)
        }
      }
      s"the user answers '$No'" should {
        s"redirect to ${routes.BusinessAccountingPeriodDateController.show().url}" in new Test {
          val request: Request[AnyContent] = subscriptionRequest.withFormUrlEncodedBody(MatchTaxYearForm.matchTaxYear -> YesNoMapping.option_no)
          val result: Result = await(controller.submit(isEditMode = false)(request))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAccountingPeriodDateController.show().url)

          verifyKeystore(fetchMatchTaxYear = 1, saveMatchTaxYear = 1)
        }
      }
      "the user does not select an answer" should {
        s"return a $BAD_REQUEST" in new Test() {
          val result: Result = await(controller.submit(isEditMode = false)(subscriptionRequest))

          status(result) mustBe BAD_REQUEST

          verifyKeystore(fetchMatchTaxYear = 0, saveMatchTaxYear = 0)
        }
      }
    }
  }

}
