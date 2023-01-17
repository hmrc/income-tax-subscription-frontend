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

package controllers.agent

import config.featureswitch.FeatureSwitch.ThrottlingFeature
import models.common.business._
import models.common.subscription.SubscriptionFailureResponse
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, Current, DateModel, Next}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import services.agent.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockAuditingService, MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.TestModels.{testAccountingMethod, testSelectedTaxYearCurrent, testValidStartDate}
import utilities.agent.TestConstants.{testARN, testCreateIncomeSources, testNino, testUtr}
import views.html.agent.AgentTaskList

import scala.concurrent.Future

class TaskListControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector {

  val accountingPeriodService: AccountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taskList: AgentTaskList = mock[AgentTaskList]

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
      mockTaskList()

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

      val result: Future[Result] = TestTaskListController.show()(subscriptionRequestWithName)

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }

  "submit" when {
    "sign up income source is successful" should {
      "return status (SEE_OTHER - 303) and redirect to the confirmation page" in {
        setupMockSubscriptionDetailsSaveFunctions()
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
        mockCreateSubscriptionFromTaskListSuccess(testARN, testNino, testUtr, testIncomeSourceModel)

        val result: Future[Result] = TestTaskListController.submit()(subscriptionRequestWithName)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationAgentController.show.url)
      }
    }

    "sign up income source fails" should {
      "return an internalServer error" in {
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
        mockCreateSubscriptionFromTaskListFailure(testARN, testNino, testUtr, testIncomeSourceModel)

        val result: Future[Result] = TestTaskListController.submit()(subscriptionRequestWithName)
        intercept[InternalServerException](await(result)).message must include(
          s"[TaskListController][submit] - failure response received from submission: ${SubscriptionFailureResponse(INTERNAL_SERVER_ERROR)}"
        )
      }
    }
  }

  "TaskListController.interpretTaxYearAsEditableOrNot" when {
    for (eligibilityNextYearOnly <- Seq(true, false)) {
      s"eligibility restricted to next year is $eligibilityNextYearOnly" when {
        "no year provided" should {
          val year = None
          val response = TaskListController.interpretTaxYearAsEditableOrNot(year, eligibilityNextYearOnly)
          if (eligibilityNextYearOnly) {
            "return next year, uneditable" in {
              response.isEmpty mustBe false
              response.get.accountingYear mustBe Next
              response.get.editable mustBe false
              response.get.confirmed mustBe false
            }
          }
          else {
            "return None" in {
              response mustBe None
            }
          }
        }
        for (specifiedYear <- Seq(Current, Next)) {
          for (specifiedConfirmed <- Seq(true, false)) {
            for (specifiedEditable <- Seq(true, false)) {
              s"$specifiedYear, $specifiedConfirmed and $specifiedEditable provided" should {
                val year = Some(AccountingYearModel(specifiedYear, confirmed = specifiedConfirmed, editable = specifiedEditable))
                val response = TaskListController.interpretTaxYearAsEditableOrNot(year, eligibilityNextYearOnly)
                if (eligibilityNextYearOnly) {
                  "return next year, uneditable" in {
                    response.isEmpty mustBe false
                    response.get.accountingYear mustBe Next
                    response.get.editable mustBe false
                    response.get.confirmed mustBe false
                  }
                }
                else {
                  s"return $specifiedYear, confirmed=false, editable=false" in {
                    response.isEmpty mustBe false
                    response.get.accountingYear mustBe specifiedYear
                    response.get.confirmed mustBe specifiedConfirmed
                    response.get.editable mustBe true
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}