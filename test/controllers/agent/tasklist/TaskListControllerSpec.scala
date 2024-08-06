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

package controllers.agent.tasklist

import config.featureswitch.FeatureSwitch.ThrottlingFeature
import controllers.agent.AgentControllerBaseSpec
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.status.MandationStatus.Voluntary
import models.{Cash, DateModel, EligibilityStatus, Next}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import services.agent.mocks.MockSubscriptionOrchestrationService
import services.mocks._
import utilities.agent.TestConstants.testUtr
import views.html.agent.tasklist.TaskList

import scala.concurrent.Future

class TaskListControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockReferenceRetrieval
  with MockUTRService
  with MockClientDetailsRetrieval {

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taskList: TaskList = mock[TaskList]

  override val controllerName: String = "TaskListController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTaskListController.show,
    "submit" -> TestTaskListController.submit
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(taskList)
    enable(ThrottlingFeature)
  }

  def mockTaskList(): Unit = {
    when(taskList(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
  }

  object TestTaskListController extends TaskListController(
    taskList,
    mockClientDetailsRetrieval,
    mockReferenceRetrieval,
    mockUTRService,
    MockSubscriptionDetailsService
  )(
    mockAuditingService,
    mockAuthService
  )

  "show" should {
    "return an OK status with the task list page" in {
      mockTaskList()
      mockSaveIncomeSourceConfirmation(Some(true))
      mockFetchAllSelfEmployments(Seq(
        SelfEmploymentData(
          id = "id",
          businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
          businessName = Some(BusinessNameModel("business name")),
          businessTradeName = Some(BusinessTradeNameModel("business trade")),
          businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
        )
      ))
      mockFetchProperty(Some(PropertyModel(
        accountingMethod = Some(Cash),
        startDate = Some(DateModel("1", "1", "1980")),
        confirmed = true
      )))
      mockFetchOverseasProperty(Some(OverseasPropertyModel(
        accountingMethod = Some(Cash),
        startDate = Some(DateModel("1", "1", "1980")),
        confirmed = true
      )))
      mockFetchSelectedTaxYear(Some(AccountingYearModel(Next)))
      mockGetMandationService(Voluntary, Voluntary)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
      mockGetUTR(testUtr)

      val result: Future[Result] = TestTaskListController.show()(subscriptionRequestWithName)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }

  "submit" when {
    "redirect to the global check your answers page" in {
      val result: Future[Result] = TestTaskListController.submit()(subscriptionRequestWithName)

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.GlobalCheckYourAnswersController.show.url)
    }
  }
}