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

import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

import scala.concurrent.Future

class YourIncomeSourceToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  override val controllerName: String = "IncomeSourceController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val incomeSources: IncomeSources = IncomeSources(
    selfEmployments = Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
    selfEmploymentAccountingMethod = Some(Cash),
    ukProperty = Some(testUkProperty),
    foreignProperty = Some(testForeignProperty)
  )

  "show" should {
    "return OK and provide the view with the fetched income sources and a false prepopulated flag" when {
      "fetching the pre pop flag returned no data" in new Setup {
        mockFetchAllIncomeSources(incomeSources = incomeSources)
        mockFetchPrePopFlag(flag = None)
        mockYourIncomeSourceToSignUpView(incomeSources = incomeSources, prepopulated = false)

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "return OK and provide the view with the fetched income sources and a false prepopulated flag" when {
      "fetching the pre pop flag returned a false flag" in new Setup {
        mockFetchAllIncomeSources(incomeSources = incomeSources)
        mockFetchPrePopFlag(flag = Some(false))
        mockYourIncomeSourceToSignUpView(incomeSources = incomeSources, prepopulated = false)

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
    "return OK and provide the view with the fetched income sources and a true prepopulated flag" when {
      "fetching the pre pop flag returned a true flag" in new Setup {
        mockFetchAllIncomeSources(incomeSources = incomeSources)
        mockFetchPrePopFlag(flag = Some(true))
        mockYourIncomeSourceToSignUpView(incomeSources = incomeSources, prepopulated = true)

        val result: Result = await(controller.show()(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }

  }

  "submit" should {
    "redirect to the task list page and save the income sources section as complete" when {
      "all income sources are complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
            selfEmploymentAccountingMethod = Some(Cash),
            ukProperty = Some(testUkProperty),
            foreignProperty = Some(testForeignProperty)
          )
        )
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation()
      }
      "only self employment income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
            selfEmploymentAccountingMethod = Some(Cash),
            ukProperty = None,
            foreignProperty = None
          )
        )
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation()
      }
      "only uk property income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq.empty,
            selfEmploymentAccountingMethod = None,
            ukProperty = Some(testUkProperty),
            foreignProperty = None
          )
        )
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation()
      }
      "only foreign property income sources are added and complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq.empty,
            selfEmploymentAccountingMethod = None,
            ukProperty = None,
            foreignProperty = Some(testForeignProperty)
          )
        )
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation()
      }
    }
    "redirect to the task list page" when {
      "self employment income sources are not complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq(testSelfEmployment("id").copy(confirmed = false)),
            selfEmploymentAccountingMethod = None,
            ukProperty = Some(testUkProperty),
            foreignProperty = Some(testForeignProperty)
          )
        )

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(0)
      }
      "uk property income sources are not complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
            selfEmploymentAccountingMethod = Some(Cash),
            ukProperty = Some(testUkProperty.copy(confirmed = false)),
            foreignProperty = Some(testForeignProperty)
          )
        )

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(0)
      }
      "overseas property income sources are not complete" in new Setup {
        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq(testSelfEmployment("id"), testSelfEmployment("id2")),
            selfEmploymentAccountingMethod = Some(Cash),
            ukProperty = Some(testUkProperty),
            foreignProperty = Some(testForeignProperty.copy(confirmed = false))
          )
        )

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(0)
      }
      "no income sources have been added" in new Setup {
        mockFetchAllSelfEmployments(Seq.empty[SelfEmploymentData])
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        mockFetchAllIncomeSources(
          IncomeSources(
            selfEmployments = Seq.empty,
            selfEmploymentAccountingMethod = None,
            ukProperty = None,
            foreignProperty = None
          )
        )

        val result: Future[Result] = controller.submit(subscriptionRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)

        verifySaveIncomeSourceConfirmation(0)
      }
    }
  }

  "backUrl" when {
    "Prepopulate feature switch is enabled" should {
      "go to the ORM page" in new Setup {
        enable(PrePopulate)
        controller.backUrl mustBe controllers.agent.routes.WhatYouNeedToDoController.show().url
      }
    }
    "Prepopulate feature switch is disabled" should {
      "go to the Task List Page" in new Setup {
        controller.backUrl mustBe controllers.agent.tasklist.routes.TaskListController.show().url
      }
    }
  }

  trait Setup {
    val yourIncomeSourceToSignUpView: YourIncomeSourceToSignUp = mock[YourIncomeSourceToSignUp]

    val controller = new YourIncomeSourceToSignUpController(
      yourIncomeSourceToSignUpView,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner,
      mockSubscriptionDetailsService
    )(appConfig)

    def mockYourIncomeSourceToSignUpView(incomeSources: IncomeSources, prepopulated: Boolean): Unit = {
      when(yourIncomeSourceToSignUpView(
        ArgumentMatchers.eq(routes.YourIncomeSourceToSignUpController.submit),
        ArgumentMatchers.eq(controllers.agent.tasklist.routes.TaskListController.show().url),
        ArgumentMatchers.any(),
        ArgumentMatchers.eq(incomeSources),
        ArgumentMatchers.eq(prepopulated)
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
