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

package controllers.agent.tasklist.addbusiness

import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.agent.AgentControllerBaseSpec
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

import scala.concurrent.Future

class YourIncomeSourceToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockAuditingService {
  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()


  "show" should {
    "return OK status" when {
      "there are no income sources added" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty)
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        mockYourIncomeSourceToSignUpView(IncomeSources(Seq.empty[SelfEmploymentData], None, None))

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there are multiple different income sources added" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto),
          testSelfEmployment("id2").encrypt(crypto.QueryParameterCrypto)
        ))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))

        mockYourIncomeSourceToSignUpView(
          IncomeSources(
            selfEmployments = Seq(
              testSelfEmployment("id"),
              testSelfEmployment("id2")
            ),
            ukProperty = Some(testUkProperty),
            foreignProperty = Some(testForeignProperty)
          )
        )

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" should {
    "redirect to the task list page and save the income sources section as complete" when {
      "all income sources are complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto),
          testSelfEmployment("id2").encrypt(crypto.QueryParameterCrypto)
        ))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only self employment income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto),
          testSelfEmployment("id2").encrypt(crypto.QueryParameterCrypto)
        ))
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only uk property income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(None)
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only foreign property income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
    }
    "redirect to the task list page" when {
      "self employment income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").copy(confirmed = false).encrypt(crypto.QueryParameterCrypto)))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "uk property income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto)))
        mockFetchProperty(Some(testUkProperty.copy(confirmed = false)))
        mockFetchOverseasProperty(Some(testForeignProperty))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "overseas property income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto)))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty.copy(confirmed = false)))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "no income sources have been added" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
    }
  }

  "backUrl" should {
    "go to the Task List Page" in new Setup {
      controller.backUrl mustBe controllers.agent.tasklist.routes.TaskListController.show().url
    }
  }

  trait Setup {
    val yourIncomeSourceToSignUpView: YourIncomeSourceToSignUp = mock[YourIncomeSourceToSignUp]

    val controller = new YourIncomeSourceToSignUpController(
      yourIncomeSourceToSignUpView,
      MockSubscriptionDetailsService,
      mockAuditingService,
      mockAuthService
    )

    def mockYourIncomeSourceToSignUpView(incomeSources: IncomeSources): Unit = {
      when(yourIncomeSourceToSignUpView(
        ArgumentMatchers.eq(routes.YourIncomeSourceToSignUpController.submit),
        ArgumentMatchers.eq(controllers.agent.tasklist.routes.TaskListController.show().url),
        ArgumentMatchers.any(),
        ArgumentMatchers.eq(incomeSources)
      )(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(HtmlFormat.empty)
    }
  }

  def testSelfEmployment(id: String): SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
    businessName = Some(BusinessNameModel("business name")),
    businessTradeName = Some(BusinessTradeNameModel("business trade")),
    businessAddress = Some(BusinessAddressModel(Address(
      lines = Seq(
        "1 long road",
        "lonely street"
      ),
      postcode = Some("ZZ1 1ZZ")
    ))),
    confirmed = true
  )

  def testUkProperty: PropertyModel = PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("2", "2", "1981")),
    confirmed = true
  )

  def testForeignProperty: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("3", "3", "1982")),
    confirmed = true
  )


}
