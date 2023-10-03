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

import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.agent.{YourIncomeSourceToSignUpController, routes}
import forms.agent.HaveYouCompletedThisSectionForm
import forms.submapping.YesNoMapping
import models.{Cash, DateModel}
import models.common.{OverseasPropertyModel, PropertyModel}
import models.common.business.{Address, BusinessAddressModel, BusinessNameModel, BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.agent.YourIncomeSourceToSignUp

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

        mockYourIncomeSourceToSignUpView(
          selfEmployments = Seq.empty,
          ukProperty = None,
          foreignProperty = None
        )

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
          selfEmployments = Seq(
            testSelfEmployment("id"),
            testSelfEmployment("id2")
          ),
          ukProperty = Some(testUkProperty),
          foreignProperty = Some(testForeignProperty)
        )

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "redirect to the task list page when selected yes" in new Setup {
      mockSaveIncomeSrouceConfirmation("test-reference")
      val result: Future[Result] = controller.submit(subscriptionRequest.withFormUrlEncodedBody(HaveYouCompletedThisSectionForm.fieldName -> YesNoMapping.option_yes))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
    }

    "redirect to the task list page when selected no" in new Setup {
      mockSaveIncomeSrouceConfirmation("test-reference")
      val result: Future[Result] = controller.submit(subscriptionRequest.withFormUrlEncodedBody(HaveYouCompletedThisSectionForm.fieldName -> YesNoMapping.option_no))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
    }
  }

  "backUrl" should {
    "go to the Task List Page" in new Setup {
      controller.backUrl mustBe controllers.agent.routes.TaskListController.show().url
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

    def mockYourIncomeSourceToSignUpView(selfEmployments: Seq[SelfEmploymentData],
                                         ukProperty: Option[PropertyModel],
                                         foreignProperty: Option[OverseasPropertyModel]): Unit = {
      when(yourIncomeSourceToSignUpView(
        ArgumentMatchers.eq(routes.YourIncomeSourceToSignUpController.submit),
        ArgumentMatchers.eq(controllers.agent.routes.TaskListController.show().url),
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.eq(selfEmployments),
        ArgumentMatchers.eq(ukProperty),
        ArgumentMatchers.eq(foreignProperty)
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
