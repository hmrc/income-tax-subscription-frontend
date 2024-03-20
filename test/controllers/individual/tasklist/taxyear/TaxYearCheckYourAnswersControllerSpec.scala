/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.individual.tasklist.taxyear

import controllers.individual.ControllerBaseSpec
import models.Current
import models.common.AccountingYearModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockSessionDataService, MockSubscriptionDetailsService}
import views.agent.mocks.MockWhatYearToSignUp
import views.html.individual.tasklist.taxyear.TaxYearCheckYourAnswers

import scala.concurrent.Future

class TaxYearCheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockWhatYearToSignUp
  with MockAuditingService
  with MockSessionDataService
  with MockAccountingPeriodService
  with MockSubscriptionDetailsService {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  "backUrl" should {
    "go to the Task List Page when isEditMode equals to true" in withController { controller =>
      controller.backUrl(true) mustBe controllers.individual.tasklist.routes.TaskListController.show().url
    }
    "go to the What Year to SignUp Page when isEditMode equals to false" in withController { controller =>
      controller.backUrl(false) mustBe
        controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
    }
  }

  "show" should {
    "return an OK status with the check your answers page" in withController { controller =>
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)

      verifyFetchSelectedTaxYear(1, "test-reference")
    }
  }

  "submit" should {
    "redirect to the task list when the submission is successful" in withController { controller =>
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
      setupMockSubscriptionDetailsSaveFunctions()

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)
      verifySaveSelectedTaxYear(1, "test-reference")
      verifyFetchSelectedTaxYear(1, "test-reference")
    }

    "throw an exception" when {
      "accounting year cannot be retrieved" in withController { controller =>
        mockFetchSelectedTaxYear(None)

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }

      "accounting year cannot be confirmed" in withController { controller =>
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  private def withController(testCode: TaxYearCheckYourAnswersController => Any): Unit = {
    val checkYourAnswersView = mock[TaxYearCheckYourAnswers]

    when(checkYourAnswersView(any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new TaxYearCheckYourAnswersController(
      checkYourAnswersView,
      accountingPeriodService
    )(
      mockAuditingService,
      appConfig,
      mockAuthService,
      mockSessionDataService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }
}
