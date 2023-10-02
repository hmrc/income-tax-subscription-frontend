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

package controllers.agent.business

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import controllers.agent.AgentControllerBaseSpec
import models.common.PropertyModel
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys
import views.html.agent.business.PropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with MockIncomeTaxSubscriptionConnector {

  override val controllerName: String = "PropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyCheckYourAnswersController.show(isEditMode = false),
    "submit" -> TestPropertyCheckYourAnswersController.submit()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(featureSwitch = EnableTaskListRedesign)
  }

  object TestPropertyCheckYourAnswersController extends PropertyCheckYourAnswersController(
    mock[PropertyCheckYourAnswers],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService
  )

  "show" should {
    "return an InternalServerException" when {
      "there are missing client details in session" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

        intercept[InternalServerException](await(controller.show(false)(subscriptionRequest)))
          .message mustBe "[IncomeTaxAgentUser][clientDetails] - could not retrieve client details from session"
      }
    }
    "return an OK status with the property CYA page" in withController { controller =>
      mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequestWithName))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception if cannot retrieve property details" in withController { controller =>
      mockFetchProperty(None)

      val result: Future[Result] = await(controller.show(false)(subscriptionRequestWithName))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }
  }

  "submit" should {
    "redirect to the task list when the submission is successful" when {
      "the user submits valid full data" in withController { controller =>
        enable(EnableTaskListRedesign)
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
        setupMockSubscriptionDetailsSaveFunctions()

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
        verifyPropertySave(Some(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true)))
      }

      "the user submits valid partial data" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
        verify(mockConnector, never).saveSubscriptionDetails[PropertyModel](
          any(),
          ArgumentMatchers.eq(SubscriptionDataKeys.Property),
          any()
        )(any(), any())
      }
    }

    "throw an exception" when {
      "cannot retrieve property details" in withController { controller =>
        mockFetchProperty(None)

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }

      "cannot confirm property details" in withController { controller =>
        mockFetchProperty(None)
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "back url" should {
    "go to the task list page" when {
      "in edit mode" in withController { controller =>
        controller.backUrl(isEditMode = true) mustBe controllers.agent.routes.TaskListController.show().url
      }
    }

    "go to the property accounting method page" when {
      "not in edit mode" in withController { controller =>
        controller.backUrl(isEditMode = false) mustBe routes.PropertyAccountingMethodController.show().url
      }
    }
  }

  private def withController(testCode: PropertyCheckYourAnswersController => Any): Unit = {
    val mockView = mock[PropertyCheckYourAnswers]

    when(mockView(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyCheckYourAnswersController(
      mockView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }
}
