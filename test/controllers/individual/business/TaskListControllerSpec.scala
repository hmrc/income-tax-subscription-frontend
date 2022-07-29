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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import controllers.ControllerBaseSpec
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel, Next}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService, MockThrottlingConnector}
import services.{AccountingPeriodService, ThrottlingService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, MtditId}
import utilities.TestModels.{testAccountingMethod, testCacheMap, testCacheMapIndiv, testSelectedTaxYearCurrent, testValidStartDate}
import utilities.individual.TestConstants.{testCreateIncomeSources, testNino}
import views.html.individual.incometax.business.TaskList

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector
  with MockThrottlingConnector {

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taskList: TaskList = mock[TaskList]

  override val controllerName: String = "TaskListController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTaskListController.show,
    "submit" -> TestTaskListController.submit
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ThrottlingFeature)
    reset(taskList)
    enable(ThrottlingFeature)
    notThrottled()
  }

  def mockTaskList(): Unit = {
    when(taskList(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)
  }

  object TestTaskListController extends TaskListController(
    taskList,
    accountingPeriodService,
    mockAuditingService,
    MockSubscriptionDetailsService,
    mockSubscriptionOrchestrationService,
    mockIncomeTaxSubscriptionConnector,
    mockAuthService,
    new ThrottlingService(mockThrottlingConnector, appConfig)
  )

  "show" should {
    "return an OK status with the task list page" in {
      mockTaskList()

      val testBusinessCacheMap = testCacheMap()
      mockFetchAllFromSubscriptionDetails(Some(testBusinessCacheMap))
      mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(Seq(
        SelfEmploymentData(
          id = "id",
          businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
          businessName = Some(BusinessNameModel("business name")),
          businessTradeName = Some(BusinessTradeNameModel("business trade")),
          businessAddress = Some(BusinessAddressModel("123", Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
        )
      ))
      mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(Some(AccountingMethodModel(Cash)))
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

      val result: Future[Result] = TestTaskListController.show()(subscriptionRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
      verifyGetThrottleStatusCalls(times(1))
    }
  }

  "submit" when {
    "sign up income source is successful" should {
      "return status (SEE_OTHER - 303) and redirect to the confirmation page" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(Some(testCacheMapIndiv))
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(Seq.empty)
        mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)
        mockFetchProperty(Some(PropertyModel(
          accountingMethod = Some(testAccountingMethod.accountingMethod),
          startDate = Some(testValidStartDate),
          confirmed = true
        )))
        mockFetchOverseasProperty(Some(OverseasPropertyModel(
          accountingMethod = Some(testAccountingMethod.accountingMethod),
          startDate = Some(testValidStartDate),
          confirmed = true
        )))
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))

        val testIncomeSourceModel = testCreateIncomeSources.copy(soleTraderBusinesses = None)
        mockSignUpAndCreateIncomeSourcesFromTaskListSuccess(testNino, testIncomeSourceModel)

        val result: Future[Result] = TestTaskListController.submit()(subscriptionRequest)
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifySubscriptionDetailsSave(MtditId, 1)
        verifyGetThrottleStatusCalls(times(1))

        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show.url)
      }
    }

    "sign up income source fails" should {
      "return an internalServer error" in {
        mockFetchAllFromSubscriptionDetails(Some(testCacheMapIndiv))
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(Seq.empty)
        mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)
        mockFetchProperty(Some(PropertyModel(
          accountingMethod = Some(testAccountingMethod.accountingMethod),
          startDate = Some(testValidStartDate),
          confirmed = true
        )))
        mockFetchOverseasProperty(Some(OverseasPropertyModel(
          accountingMethod = Some(testAccountingMethod.accountingMethod),
          startDate = Some(testValidStartDate),
          confirmed = true
        )))
        val testIncomeSourceModel = testCreateIncomeSources.copy(soleTraderBusinesses = None)
        mockSignUpAndCreateIncomeSourcesFromTaskListFailure(testNino, testIncomeSourceModel)
        mockFetchSelectedTaxYear(Some(testSelectedTaxYearCurrent))

        val result: Future[Result] = TestTaskListController.submit()(subscriptionRequest)
        intercept[InternalServerException](await(result)).message must include("Successful response not received from submission")
        verifySubscriptionDetailsSave(MtditId, 0)
        verifyGetThrottleStatusCalls(times(1))
      }
    }
  }
}
