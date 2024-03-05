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

package controllers.individual

import models.common.business.Address
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionFailureResponse}
import models.{Cash, Current}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.GetCompleteDetailsService
import services.GetCompleteDetailsService._
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockAuditingService, MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.TestConstants.testNino
import views.html.individual.GlobalCheckYourAnswers

import java.time.LocalDate
import scala.concurrent.Future

class GlobalCheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector {

  object TestGlobalCheckYourAnswersController extends GlobalCheckYourAnswersController(
    auditingService = mockAuditingService,
    authService = mockAuthService,
    subscriptionDetailsService = MockSubscriptionDetailsService,
    subscriptionService = mockSubscriptionOrchestrationService,
    getCompleteDetailsService = mock[GetCompleteDetailsService],
    globalCheckYourAnswers = mock[GlobalCheckYourAnswers]
  )

  override val controllerName: String = "GlobalCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestGlobalCheckYourAnswersController.show,
    "submit" -> TestGlobalCheckYourAnswersController.submit
  )

  trait Setup {
    val mockGetCompleteDetailsService: GetCompleteDetailsService = mock[GetCompleteDetailsService]
    val mockGlobalCheckYourAnswers: GlobalCheckYourAnswers = mock[GlobalCheckYourAnswers]

    val controller: GlobalCheckYourAnswersController = new GlobalCheckYourAnswersController(
      auditingService = mockAuditingService,
      authService = mockAuthService,
      subscriptionDetailsService = MockSubscriptionDetailsService,
      subscriptionService = mockSubscriptionOrchestrationService,
      getCompleteDetailsService = mockGetCompleteDetailsService,
      globalCheckYourAnswers = mockGlobalCheckYourAnswers
    )
  }

  val completeDetails: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        accountingMethod = Cash,
        businesses = Seq(
          SoleTraderBusiness(
            id = "id",
            name = s"ABC",
            trade = s"Plumbing",
            startDate = LocalDate.of(1980, 1, 1),
            address = Address(
              lines = Seq(
                s"1 Long Road",
                "Lonely City"
              ),
              postcode = Some(s"ZZ11ZZ")
            )
          )
        )
      )),
      ukProperty = Some(UKProperty(
        startDate = LocalDate.of(1980, 1, 2),
        accountingMethod = Cash
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = LocalDate.of(1980, 1, 3),
        accountingMethod = Cash
      ))
    ),
    taxYear = Current
  )

  "show" must {
    "redirect to the task list page" when {
      "a failure is returned from the get complete details service" in new Setup {
        when(mockGetCompleteDetailsService.getCompleteSignUpDetails(any())(any(), any()))
          .thenReturn(Future.successful(Left(GetCompleteDetailsService.GetCompleteDetailsFailure)))

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(tasklist.routes.TaskListController.show().url)
      }
    }
    "return OK with the page content" when {
      "complete details are successfully received" in new Setup {
        when(mockGetCompleteDetailsService.getCompleteSignUpDetails(any())(any(), any()))
          .thenReturn(Future.successful(Right(completeDetails)))

        when(mockGlobalCheckYourAnswers(
          ArgumentMatchers.eq(routes.GlobalCheckYourAnswersController.submit),
          ArgumentMatchers.eq(tasklist.routes.TaskListController.show().url),
          ArgumentMatchers.eq(completeDetails)
        )(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(subscriptionRequest)

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "redirect to the task list page" when {
      "a failure is returned from the get complete details service" in new Setup {
        when(mockGetCompleteDetailsService.getCompleteSignUpDetails(any())(any(), any()))
          .thenReturn(Future.successful(Left(GetCompleteDetailsService.GetCompleteDetailsFailure)))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(tasklist.routes.TaskListController.show().url)
      }
    }

    "redirect to the confirmation page" when {
      "the submission of the users details was successful" in new Setup {
        when(mockGetCompleteDetailsService.getCompleteSignUpDetails(any())(any(), any()))
          .thenReturn(Future.successful(Right(completeDetails)))

        mockSignUpAndCreateIncomeSourcesFromTaskListSuccess(nino = testNino, CreateIncomeSourcesModel.createIncomeSources(testNino, completeDetails))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.routes.ConfirmationController.show.url)
      }
    }

    "throw an InternalServerException" when {
      "a failure is returned from the subscription orchestration submission" in new Setup {
        when(mockGetCompleteDetailsService.getCompleteSignUpDetails(any())(any(), any()))
          .thenReturn(Future.successful(Right(completeDetails)))

        mockSignUpAndCreateIncomeSourcesFromTaskListFailure(nino = testNino, CreateIncomeSourcesModel.createIncomeSources(testNino, completeDetails))

        intercept[InternalServerException](await(controller.submit(subscriptionRequest)))
          .message mustBe s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${SubscriptionFailureResponse(INTERNAL_SERVER_ERROR)}"
      }
    }
  }

  authorisationTests()

}
