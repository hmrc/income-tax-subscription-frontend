/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.incomesource

import agent.audit.mocks.MockAuditingService
import config.MockConfig
import config.featureswitch.FeatureSwitch.{SaveAndRetrieve, ForeignProperty => ForeignPropertyFeature}
import config.featureswitch.FeatureSwitching
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.common._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.incometax.incomesource.WhatIncomeSourceToSignUp
import views.individual.mocks.MockIncomeSource

import scala.concurrent.Future

class WhatIncomeSourceToSignUpControllerSpec extends ControllerBaseSpec
  with MockIncomeSource
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockConfig
  with MockAuditingService
  with FeatureSwitching {

  override val controllerName: String = "WhatIncomeSourceToSignUpController"

  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "backUrl" should {
    "go to the Task List Page" in withController { controller =>
      controller.backUrl mustBe controllers.individual.business.routes.TaskListController.show().url
    }
  }

  "show" should {
    "return 200 OK status" in withController { controller =>
      enable(SaveAndRetrieve)

      val result = await(controller.show()(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception if Save & Retrieve is disabled" in withController { controller =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  "submit" should {
    def submit(controller: WhatIncomeSourceToSignUpController, incomeSourceModel: BusinessIncomeSourceModel): Future[Result] = {
      controller.submit()(
        subscriptionRequest.post(BusinessIncomeSourceForm.businessIncomeSourceForm(), incomeSourceModel)
      )
    }

    "redirect to the start of the self employment journey" in withController { controller =>
      setupMockSubscriptionDetailsSaveFunctions()

      val result = await(submit(controller, BusinessIncomeSourceModel(SelfEmployed)))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
    }

    "redirect to the PropertyStartDate page" in withController { controller =>
      setupMockSubscriptionDetailsSaveFunctions()

      val result = await(submit(controller, BusinessIncomeSourceModel(UkProperty)))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.individual.business.routes.PropertyStartDateController.show().url
    }

    "redirect to the overseas property start date page" in withController { controller =>
      enable(ForeignPropertyFeature)
      setupMockSubscriptionDetailsSaveFunctions()

      val result = await(submit(controller, BusinessIncomeSourceModel(ForeignProperty)))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).get mustBe controllers.individual.business.routes.OverseasPropertyStartDateController.show().url
    }

    "throw an exception if Foreign property is disabled" in withController { controller =>
      disable(ForeignPropertyFeature)

      intercept[InternalServerException](await(
        submit(controller, BusinessIncomeSourceModel(ForeignProperty))
      )).message must include("[WhatIncomeSourceToSignUpController][submit] - The foreign property feature switch is disabled")
    }
  }

  private def withController(testCode: WhatIncomeSourceToSignUpController => Any) = {
    val whatIncomeSourceToSignUpView = mock[WhatIncomeSourceToSignUp]

    when(whatIncomeSourceToSignUpView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new WhatIncomeSourceToSignUpController(
      whatIncomeSourceToSignUpView,
      mockAuditingService,
      mockAuthService
    )

    testCode(controller)
  }
}
