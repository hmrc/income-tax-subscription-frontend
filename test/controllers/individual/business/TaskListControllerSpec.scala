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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.Assets.{OK, SEE_OTHER}
import controllers.ControllerBaseSpec
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.SubscriptionDataKeys.MtditId
import utilities.TestModels.testCacheMapIndiv
import utilities.individual.TestConstants.{testCreateIncomeSources, testNino}
import views.html.individual.incometax.business.TaskList

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector
  with FeatureSwitching {

  override val controllerName: String = "TaskListController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTaskListController.show,
    "submit" -> TestTaskListController.submit
  )

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    reset(taskListView)
    super.beforeEach()
  }

  def taskListView = {
    val mockTaskListView: TaskList = mock[TaskList]
    when(mockTaskListView(any(), any())(any(), any(), any())).thenReturn(HtmlFormat.empty)
    mockTaskListView
  }

  object TestTaskListController extends TaskListController(
    taskListView,
    mockAuditingService,
    mockAuthService,
    mockSubscriptionOrchestrationService,
    MockSubscriptionDetailsService,
    mockIncomeTaxSubscriptionConnector
  )


  "show" should {
    "return an OK status with the task list page" in {
      enable(SaveAndRetrieve)
      val result: Future[Result] = TestTaskListController.show()(subscriptionRequest)
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "return SEE_OTHER status when unauthorised" in {
      enable(SaveAndRetrieve)
      val result: Future[Result] = TestTaskListController.show()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }

    "Throw an exception if feature not enabled" in {
      disable(SaveAndRetrieve)
      val result: Future[Result] = await(TestTaskListController.show()(subscriptionRequest))
      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  "if submit button shows" when {
    "save and retrieve and Sps feature switch is enabled" when {
      "the submission is successful" should {
        "return status (SEE_OTHER - 303) and redirect to the confirmation page" in {
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
          mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
          mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
          val testIncomeSourceModel = testCreateIncomeSources.copy(selfEmployments = None, overseasProperty = None)
          mockSignUpAndCreateIncomeSourcesFromTaskListSuccess(testNino, testIncomeSourceModel)

          val result: Future[Result] = TestTaskListController.submit()(subscriptionRequest)
          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifySubscriptionDetailsSave(MtditId, 1)
          verifySubscriptionDetailsFetchAll(2)

          redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
        }
      }

      "the submission is failed" should {
        "return an internalServer error" in {
          enable(SaveAndRetrieve)
          mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
          mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
          mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
          val testIncomeSourceModel = testCreateIncomeSources.copy(selfEmployments = None, overseasProperty = None)
          mockSignUpAndCreateIncomeSourcesFromTaskListFailure(testNino, testIncomeSourceModel)

          val result: Future[Result] = TestTaskListController.submit()(subscriptionRequest)
          intercept[InternalServerException](await(result)).message must include("Successful response not received from submission")
          verifySubscriptionDetailsFetchAll(1)
          verifySubscriptionDetailsSave(MtditId, 0)
        }
      }

    }
    "save and retrieve feature switch is disabled" should {
      "throw NotFoundException" in {

        mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
        mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
        mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)

        intercept[NotFoundException](await(TestTaskListController.submit()(subscriptionRequest)))
          .getMessage mustBe "[TaskListController][submit] - The save and retrieve feature switch is disabled"

      }
    }
  }
}
