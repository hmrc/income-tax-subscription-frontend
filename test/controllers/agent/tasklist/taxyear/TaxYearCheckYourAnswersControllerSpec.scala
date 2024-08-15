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

package controllers.agent.tasklist.taxyear

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import models.common.AccountingYearModel
import models.status.MandationStatus.Voluntary
import models.{Current, EligibilityStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import views.agent.mocks.MockWhatYearToSignUp
import views.html.agent.tasklist.taxyear.TaxYearCheckYourAnswers

import scala.concurrent.Future

class TaxYearCheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockWhatYearToSignUp
  with MockAuditingService
  with MockAccountingPeriodService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval
  with MockSubscriptionDetailsService {

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "backUrl" should {
    "go to the Task List Page when isEditMode equals to true" in withController { controller =>
      controller.backUrl(true) mustBe controllers.agent.tasklist.routes.TaskListController.show().url
    }
    "go to the What Year to SignUp Page when isEditMode equals to false" in withController { controller =>
      controller.backUrl(false) mustBe
        controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
    }
  }

  "show" should {
    "return an OK status with the check your answers page" in withController { controller =>
      mockGetMandationService(Voluntary, Voluntary)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequestWithName))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }

  "submit" should {
    "redirect to the task list when the submission is successful" in withController { controller =>
      mockGetMandationService(Voluntary, Voluntary)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
      mockSaveSelectedTaxYear(AccountingYearModel(Current, confirmed = true))(Right(PostSubscriptionDetailsSuccessResponse))

      val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)
    }

    "throw an exception" when {
      "the accounting year cannot be retrieved" in withController { controller =>
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(None)

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }

      "the accounting year cannot be confirmed" in withController { controller =>
        mockGetMandationService(Voluntary, Voluntary)
        mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        mockFetchSelectedTaxYear(Some(AccountingYearModel(Current)))
        mockSaveSelectedTaxYear(AccountingYearModel(Current, confirmed = true))(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )


        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  private def withController(testCode: TaxYearCheckYourAnswersController => Any): Unit = {
    val checkYourAnswersView = mock[TaxYearCheckYourAnswers]

    when(checkYourAnswersView(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new TaxYearCheckYourAnswersController(
      checkYourAnswersView,
      mockSubscriptionDetailsService,
      mockClientDetailsRetrieval,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      mockGetEligibilityStatusService,
      mockMandationStatusService
    )

    testCode(controller)
  }
}
