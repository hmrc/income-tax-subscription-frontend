/*
 * Copyright 2022 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.agent.{AgentControllerBaseSpec, TaxYearCheckYourAnswersController}
import models.Current
import models.common.AccountingYearModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAccountingPeriodService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.{MtditId, SelectedTaxYear}
import views.agent.mocks.MockWhatYearToSignUp
import views.html.agent.business.TaxYearCheckYourAnswers

import scala.concurrent.Future

class TaxYearCheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockWhatYearToSignUp
  with MockAuditingService
  with MockAccountingPeriodService
  with MockSubscriptionDetailsService
  with FeatureSwitching {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "backUrl" should {
    "go to the Task List Page when isEditMode equals to true" in withController { controller =>
      controller.backUrl(true) mustBe controllers.agent.routes.TaskListController.show().url
    }
    "go to the What Year to SignUp Page when isEditMode equals to false" in withController { controller =>
      controller.backUrl(false) mustBe
        controllers.agent.routes.WhatYearToSignUpController.show().url
    }
  }
  "show" should {
    "return an OK status with the check your answers page" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchSelectedTaxYearFromSubscriptionDetails(Some(AccountingYearModel(Current)))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)

      verifySubscriptionDetailsFetch(SelectedTaxYear, 1)
    }

    "throw an exception if feature not enabled" in withController { controller =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  "submit" should {
    "redirect to the task list when the submission is successful" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchSelectedTaxYearFromSubscriptionDetails(Some(AccountingYearModel(Current)))
      setupMockSubscriptionDetailsSaveFunctions()

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
      verifySubscriptionDetailsSave(SelectedTaxYear, 1)
      verifySubscriptionDetailsFetch(SelectedTaxYear, 2)
    }

    "throw an exception if cannot retrieve accounting year" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchSelectedTaxYearFromSubscriptionDetails(None)

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      verifySubscriptionDetailsSave(MtditId, 0)
    }
  }

  private def withController(testCode: TaxYearCheckYourAnswersController => Any): Unit = {
    val checkYourAnswersView = mock[TaxYearCheckYourAnswers]

    when(checkYourAnswersView(any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new TaxYearCheckYourAnswersController(
      checkYourAnswersView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }
}
