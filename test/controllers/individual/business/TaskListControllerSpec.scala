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

package controllers.individual.business

import config.featureswitch.FeatureSwitch.ThrottlingFeature
import controllers.ControllerBaseSpec
import models.common.business._
import models.common.subscription.SubscriptionFailureResponse
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel, Next}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.AccountingPeriodService
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockAuditingService, MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.TestModels.{testAccountingMethod, testSelectedTaxYearCurrent, testValidStartDate}
import utilities.individual.TestConstants.{testCreateIncomeSources, testNino}
import views.html.individual.incometax.business.TaskList

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector {

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
  }

  def mockTaskList(): Unit = {
    when(taskList(any(), any(), any(), any(), any(), any())(any(), any(), any()))
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

      mockFetchAllSelfEmployments(Seq(
        SelfEmploymentData(
          id = "id",
          businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
          businessName = Some(BusinessNameModel("business name").encrypt(crypto.QueryParameterCrypto)),
          businessTradeName = Some(BusinessTradeNameModel("business trade")),
          businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))).encrypt(crypto.QueryParameterCrypto))
        )
      ))
      mockFetchSelfEmploymentAccountingMethod(Some(AccountingMethodModel(Cash)))
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
    }
  }

  "submit" when {
    "sign up income source is successful" should {
      "return status (SEE_OTHER - 303) and redirect to the confirmation page" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllSelfEmployments(Seq.empty)
        mockFetchSelfEmploymentAccountingMethod(None)
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
        await(result)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show.url)
      }
    }

    "sign up income source fails" should {
      "return an internalServer error" in {
        mockFetchAllSelfEmployments(Seq.empty)
        mockFetchSelfEmploymentAccountingMethod(None)
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
        intercept[InternalServerException](await(result)).message must include(
          s"[TaskListController][submit] - failure response received from submission: ${SubscriptionFailureResponse(INTERNAL_SERVER_ERROR)}"
        )
      }
    }
  }
}
