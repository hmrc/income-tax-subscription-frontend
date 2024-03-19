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

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import controllers.agent.AgentControllerBaseSpec
import forms.agent.BusinessIncomeSourceForm
import models.common._
import models.common.business.SelfEmploymentData
import models.{Accruals, Cash, IncomeSourcesStatus}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSessionDataService, MockSubscriptionDetailsService}
import views.html.agent.tasklist.addbusiness.WhatIncomeSourceToSignUp

import scala.concurrent.Future

class WhatIncomeSourceToSignUpControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockSessionDataService
  with MockAuditingService {
  override val controllerName: String = "IncomeSourceController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return 200 OK status if _anything_ is available and foreign property is allowed" when {
      List(
        IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = false),
        IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = true),
        IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = true),
        IncomeSourcesStatus(selfEmploymentAvailable = true, ukPropertyAvailable = false, overseasPropertyAvailable = false),
        IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = true, overseasPropertyAvailable = false),
        IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = false, overseasPropertyAvailable = true)
      ) foreach { incomeSourcesStatus =>
        s"self employment available = ${incomeSourcesStatus.selfEmploymentAvailable}," +
          s"uk property available = ${incomeSourcesStatus.ukPropertyAvailable} and" +
          s"overseas property available = ${incomeSourcesStatus.overseasPropertyAvailable}" in withController { controller =>
          enable(ForeignPropertyFeature)

          mockIncomeSourcesStatus(incomeSourcesStatus)

          val result = await(controller.show()(subscriptionRequest))

          status(result) mustBe OK
          contentType(result) mustBe Some(HTML)
        }
      }
    }
    "redirect to task list" when {
      "only foreign property is available but foreign property is not allowed" when {
        val incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = false, overseasPropertyAvailable = true)
        s"self employment available = ${incomeSourcesStatus.selfEmploymentAvailable}," +
          s"uk property available = ${incomeSourcesStatus.ukPropertyAvailable} and" +
          s"overseas property available = ${incomeSourcesStatus.overseasPropertyAvailable}" in withController { controller =>
          disable(ForeignPropertyFeature)

          mockIncomeSourcesStatus(incomeSourcesStatus)

          val result = await(controller.show()(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.agent.tasklist.routes.TaskListController.show().url
        }
      }
      "nothing is available even though foreign property is allowed" when {
        val incomeSourcesStatus = IncomeSourcesStatus(selfEmploymentAvailable = false, ukPropertyAvailable = false, overseasPropertyAvailable = false)
        s"self employment available = ${incomeSourcesStatus.selfEmploymentAvailable}," +
          s"uk property available = ${incomeSourcesStatus.ukPropertyAvailable} and" +
          s"overseas property available = ${incomeSourcesStatus.overseasPropertyAvailable}" in withController { controller =>
          enable(ForeignPropertyFeature)

          mockIncomeSourcesStatus(incomeSourcesStatus)

          val result = await(controller.show()(subscriptionRequest))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.agent.tasklist.routes.TaskListController.show().url
        }
      }
    }
  }

  "submit" should {
    def submit(controller: WhatIncomeSourceToSignUpController,
               incomeSourceModel: BusinessIncomeSource,
               incomeSourcesStatus: IncomeSourcesStatus = IncomeSourcesStatus(
                 selfEmploymentAvailable = true,
                 ukPropertyAvailable = true,
                 overseasPropertyAvailable = true
               )): Future[Result] = {

      controller.submit()(
        subscriptionRequest.post(BusinessIncomeSourceForm.businessIncomeSourceForm(incomeSourcesStatus), incomeSourceModel)
      )

    }

    "redirect to the start of the self employment journey" in withController { controller =>
      setupMockSubscriptionDetailsSaveFunctions()
      mockIncomeSourcesStatus()

      val result = await(submit(controller, SelfEmployed))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
    }

    "redirect to the PropertyStartDate page" in withController { controller =>
      setupMockSubscriptionDetailsSaveFunctions()
      mockIncomeSourcesStatus()

      val result = await(submit(controller, UkProperty))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    }

    "redirect to the overseas property start date page" in withController { controller =>
      enable(ForeignPropertyFeature)

      setupMockSubscriptionDetailsSaveFunctions()
      mockIncomeSourcesStatus()

      val result = await(submit(controller, OverseasProperty))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url
    }

    "return a BAD_REQUEST (400)" when {
      "no option was selected" in withController { controller =>
        enable(ForeignPropertyFeature)

        mockIncomeSourcesStatus()

        val result = controller.submit()(subscriptionRequest)

        status(result) mustBe Status.BAD_REQUEST
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  private def withController(testCode: WhatIncomeSourceToSignUpController => Assertion) = {
    val whatIncomeSourceToSignUpView = mock[WhatIncomeSourceToSignUp]

    when(whatIncomeSourceToSignUpView(
      any(),
      any(),
      meq(controllers.agent.tasklist.addbusiness.routes.WhatIncomeSourceToSignUpController.submit()),
      meq(controllers.agent.tasklist.routes.TaskListController.show().url)
    )(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new WhatIncomeSourceToSignUpController(
      whatIncomeSourceToSignUpView
    )(
      MockSubscriptionDetailsService,
      mockAuditingService,
      mockSessionDataService,
      mockAuthService
    )

    testCode(controller)
  }

  private def mockIncomeSourcesStatus(incomeSourcesStatus: IncomeSourcesStatus = IncomeSourcesStatus(
    selfEmploymentAvailable = true, ukPropertyAvailable = true, overseasPropertyAvailable = true
  )): Unit = {
    if (incomeSourcesStatus.selfEmploymentAvailable) {
      mockFetchAllSelfEmployments()
    } else {
      mockFetchAllSelfEmployments(Seq.fill(appConfig.maxSelfEmployments)(SelfEmploymentData("testId")))
    }
    if (incomeSourcesStatus.ukPropertyAvailable) {
      mockFetchProperty(None)
    } else {
      mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))
    }
    if (incomeSourcesStatus.overseasPropertyAvailable) {
      mockFetchOverseasProperty(None)
    } else {
      mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Accruals))))
    }
  }
}
