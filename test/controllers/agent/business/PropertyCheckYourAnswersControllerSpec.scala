/*
 * Copyright 2022 HM Revenue & Customs
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

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import models.common.PropertyModel
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import views.html.agent.business.PropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService with MockIncomeTaxSubscriptionConnector with FeatureSwitching {

  override val controllerName: String = "PropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyCheckYourAnswersController.show(isEditMode = false),
    "submit" -> TestPropertyCheckYourAnswersController.submit()
  )

  object TestPropertyCheckYourAnswersController extends PropertyCheckYourAnswersController(
    mock[PropertyCheckYourAnswers],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService
  )

  "show" should {
    "return an OK status with the property CYA page" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchProperty(Some(PropertyModel(accountingMethod = Cash)))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception if cannot retrieve property details" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchProperty(None)

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }

    "throw an exception if feature not enabled" in withController { controller =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
    }
  }

  "submit" should {
    "redirect to the task list when the submission is successful" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
      setupMockSubscriptionDetailsSaveFunctions()

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.agent.routes.TaskListController.show().url)
      verifyPropertySave(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true))
    }

    "throw an exception if cannot retrieve property details" in withController { controller =>
      enable(SaveAndRetrieve)
      mockFetchProperty(None)

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }

    "throw an exception if feature not enabled" in withController { controller =>
      disable(SaveAndRetrieve)

      val result: Future[Result] = await(controller.submit()(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
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

    when(mockView(any(), any(), any())(any(), any(), any()))
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