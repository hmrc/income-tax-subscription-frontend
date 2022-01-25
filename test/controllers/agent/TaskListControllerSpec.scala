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

package controllers.agent

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel, Next}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import services.agent.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, MtditId}
import utilities.TestModels.{testAccountingMethod, testCacheMap, testCacheMapIndiv, testValidStartDate}
import utilities.agent.TestConstants.{testARN, testCreateIncomeSources, testNino, testUtr}
import views.html.agent.AgentTaskList

import scala.concurrent.Future

class TaskListControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector
  with FeatureSwitching {

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taskList: AgentTaskList = mock[AgentTaskList]

  override val controllerName: String = "TaskListController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestTaskListController.show,
    "submit" -> TestTaskListController.submit
  )

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    reset(taskList)
    super.beforeEach()
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
    mockAuthService
  )

  "show" should {
    "return an OK status with the task list page" in {
      enable(SaveAndRetrieve)
      mockTaskList()

      val testBusinessCacheMap = testCacheMap(
        selectedTaxYear = Some(AccountingYearModel(Next))
      )
      mockFetchAllFromSubscriptionDetails(testBusinessCacheMap)
      mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(Some(Seq(
        SelfEmploymentData(
          id = "id",
          businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
          businessName = Some(BusinessNameModel("business name")),
          businessTradeName = Some(BusinessTradeNameModel("business trade")),
          businessAddress = Some(BusinessAddressModel("123", Address(Seq("line 1"), "ZZ1 1ZZ")))
        )
      )))
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

      val result: Future[Result] = TestTaskListController.show()(subscriptionRequestWithName)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception" when {
      "save and retrieve is disabled" in {
        disable(SaveAndRetrieve)

        val result: Future[Result] = await(TestTaskListController.show()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
      }
    }
  }

  "submit" when {
    "save and retrieve and Sps feature switch is enabled" when {
      "sign up income source is successful" should {
        "return status (SEE_OTHER - 303) and redirect to the confirmation page" in {
          enable(SaveAndRetrieve)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
          mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
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
          val testIncomeSourceModel = testCreateIncomeSources.copy(selfEmployments = None)
          mockCreateSubscriptionFromTaskListSuccess(testARN, testNino, testUtr, testIncomeSourceModel)

          val result: Future[Result] = TestTaskListController.submit()(subscriptionRequestWithName)
          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifySubscriptionDetailsSave(MtditId, 1)

          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationAgentController.show.url)
        }
      }

      "sign up income source fails" should {
        "return an internalServer error" in {
          enable(SaveAndRetrieve)
          mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
          mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
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

          val testIncomeSourceModel = testCreateIncomeSources.copy(selfEmployments = None)
          mockCreateSubscriptionFromTaskListFailure(testARN, testNino, testUtr, testIncomeSourceModel)

          val result: Future[Result] = TestTaskListController.submit()(subscriptionRequestWithName)
          intercept[InternalServerException](await(result)).message must include("Successful response not received from submission")
          verifySubscriptionDetailsSave(MtditId, 0)
        }
      }
    }

    "save and retrieve feature switch is disabled" should {
      "throw NotFoundException" in {
        mockFetchAllFromSubscriptionDetails(testCacheMapIndiv)
        mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
        mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)

        intercept[NotFoundException](await(TestTaskListController.submit()(subscriptionRequestWithName)))
          .getMessage mustBe "[TaskListController][submit] - The save and retrieve feature switch is disabled"

      }
    }
  }
}
