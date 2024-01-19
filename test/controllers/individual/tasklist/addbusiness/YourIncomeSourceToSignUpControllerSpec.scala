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

package controllers.individual.tasklist.addbusiness

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.individual.ControllerBaseSpec
import forms.individual.incomesource.HaveYouCompletedThisSectionForm
import forms.submapping.YesNoMapping
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp
import utilities.TestModels.testAccountingMethod

import scala.concurrent.Future

class YourIncomeSourceToSignUpControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockAuditingService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignPropertyFeature)
  }

  override val controllerName: String = "YourIncomeSourceToSignUpController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return OK status" when {
      "there are no income sources added" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty)
        mockFetchSelfEmploymentAccountingMethod(None)
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        mockYourIncomeSourceToSignUpView(
          selfEmployments = Seq.empty,
          ukProperty = None,
          foreignProperty = None
        )

        val result: Result = await(controller.show()(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
      "there are multiple different income sources added" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto),
          testSelfEmployment("id2").encrypt(crypto.QueryParameterCrypto)
        ))
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
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

        val result: Result = await(controller.show()(subscriptionRequest))

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
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only self employment income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(
          testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto),
          testSelfEmployment("id2").encrypt(crypto.QueryParameterCrypto)
        ))
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only uk property income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchSelfEmploymentAccountingMethod(None)
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(None)
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
      "only foreign property income sources are added and complete" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchSelfEmploymentAccountingMethod(None)
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))
        mockSaveIncomeSourceConfirmation("test-reference")

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 1)
      }
    }
    "redirect to the task list page" when {
      "self employment income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").copy(confirmed = false).encrypt(crypto.QueryParameterCrypto)))
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "uk property income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto)))
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
        mockFetchProperty(Some(testUkProperty.copy(confirmed = false)))
        mockFetchOverseasProperty(Some(testForeignProperty))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "overseas property income sources are not complete" in new Setup {
        mockFetchAllSelfEmployments(Seq(testSelfEmployment("id").encrypt(crypto.QueryParameterCrypto)))
        mockFetchSelfEmploymentAccountingMethod(Some(testAccountingMethod))
        mockFetchProperty(Some(testUkProperty))
        mockFetchOverseasProperty(Some(testForeignProperty.copy(confirmed = false)))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
      "no income sources have been added" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchSelfEmploymentAccountingMethod(None)
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(reference = "test-reference", count = 0)
      }
    }
  }

  "backUrl" should {
    "go to the Task List Page" in new Setup {
      controller.backUrl mustBe controllers.individual.tasklist.routes.TaskListController.show().url
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
        ArgumentMatchers.eq(controllers.individual.tasklist.routes.TaskListController.show().url),
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
