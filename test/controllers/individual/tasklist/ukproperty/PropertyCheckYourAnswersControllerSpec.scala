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

package controllers.individual.tasklist.ukproperty

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.individual.ControllerBaseSpec
import models.common.PropertyModel
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAccountingPeriodService, MockAuditingService, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.agent.mocks.MockWhatYearToSignUp
import views.html.individual.tasklist.ukproperty.PropertyCheckYourAnswers

import scala.concurrent.Future

class PropertyCheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockWhatYearToSignUp
  with MockAuditingService
  with MockAccountingPeriodService
  with MockReferenceRetrieval
  with MockSubscriptionDetailsService {

  override val controllerName: String = "PropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  "show" should {
    "return an OK status with the property CYA page" in withController { controller =>
      mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception if cannot retrieve property details" in withController { controller =>
      mockFetchProperty(None)

      val result: Future[Result] = await(controller.show(false)(subscriptionRequest))

      result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
    }
  }

  "submit" should {
    "redirect to the your income sources page and confirm the uk property details" when {
      "the user submits a start date and accounting method" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
        mockSaveProperty(PropertyModel(Some(Cash), Some(DateModel("10", "11", "2021")), confirmed = true))(
          Right(PostSubscriptionDetailsSuccessResponse)
        )

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
      }
    }
    "redirect to the your income sources page but don't confirm the uk property details" when {
      "the user submits partial data" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(accountingMethod = Some(Cash))))

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
      }
    }
  }

  "submit" should {
    "throw an exception" when {
      "cannot retrieve property details" in withController { controller =>
        mockFetchProperty(None)

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }

      "cannot confirm property details" in withController { controller =>
        mockFetchProperty(Some(PropertyModel(Some(Cash), Some(DateModel("1", "1", "1980")))))
        mockSaveProperty(PropertyModel(Some(Cash), Some(DateModel("1", "1", "1980")), confirmed = true))(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result: Future[Result] = await(controller.submit()(subscriptionRequest))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "backUrl" should {
    "in edit mode " when {
      "return the task list page" in withController { controller =>
        controller.backUrl(true) mustBe controllers.individual.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      }
    }
    "go to the property accounting method page" when {
      "not in edit mode" in withController { controller =>
        controller.backUrl(false) mustBe routes.PropertyAccountingMethodController.show().url
      }
    }
  }

  private def withController(testCode: PropertyCheckYourAnswersController => Any): Unit = {
    val view = mock[PropertyCheckYourAnswers]

    when(view(any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyCheckYourAnswersController(
      view,
      mockSubscriptionDetailsService,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
    )

    testCode(controller)
  }
}
