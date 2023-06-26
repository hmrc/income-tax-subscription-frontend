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

package controllers.individual.incomesource

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import forms.individual.incomesource.BusinessIncomeSourceForm
import models.common._
import models.common.business.SelfEmploymentData
import models.{Accruals, Cash, IncomeSourcesStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, await, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.html.individual.incometax.incomesource.YourIncomeSourceToSignUp

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


  "backUrl" should {
    "go to the Task List Page" in withController { controller =>
      controller.backUrl mustBe controllers.individual.business.routes.TaskListController.show().url
    }
  }

  "show" should {
    "return OK status" in {

      withController { controller =>
        val result = await(controller.show()(subscriptionRequest))
        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }

  }

    private def withController(testCode: YourIncomeSourceToSignUpController => Assertion) = {
      val yourIncomeSourceToSignUpView = mock[YourIncomeSourceToSignUp]

      when(yourIncomeSourceToSignUpView(any())(any(), any(), any()))
        .thenReturn(HtmlFormat.empty)

      val controller = new YourIncomeSourceToSignUpController(
        yourIncomeSourceToSignUpView,
        MockSubscriptionDetailsService,
        mockAuditingService,
        mockAuthService
      )

      testCode(controller)
    }

  }
