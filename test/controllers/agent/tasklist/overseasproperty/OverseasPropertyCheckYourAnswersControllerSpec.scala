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

package controllers.agent.tasklist.overseasproperty

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import models.common.OverseasPropertyModel
import models.{Cash, DateModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.html.agent.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

import scala.concurrent.Future

class OverseasPropertyCheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval {

  override val controllerName: String = "OverseasPropertyCheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyCheckYourAnswersController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyCheckYourAnswersController.submit()
  )

  "show" should {
    "return an OK status with the property CYA page" in {
      withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))

        val result: Future[Result] = await(controller.show(false)(subscriptionRequestWithName))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

    "throw an exception if cannot retrieve property details" in {
      withController { controller =>
        mockFetchOverseasProperty(None)

        val result: Future[Result] = await(controller.show(false)(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "submit" should {
    "redirect to the your income sources page" when {
      "the user answer all the answers for the overseas property" should {
        "save the overseas property answers" in {
          withController { controller =>
            mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
            mockSaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true))(
              Right(PostSubscriptionDetailsSuccessResponse)
            )

            val result: Future[Result] = controller.submit()(subscriptionRequestWithName)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
      }

      "the user answer partial answers for the overseas property" should {
        "not save the overseas property answers" in {
          withController { controller =>
            mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))

            val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
          }
        }
      }
    }
  }

  "submit" should {
    "throw an exception" when {
      "cannot retrieve property details" in {
        withController { controller =>
          mockFetchOverseasProperty(None)

          val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

          result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
        }
      }

      "cannot confirm overseas property details" in withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")))))
        mockSaveOverseasProperty(OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("10", "11", "2021")), confirmed = true))(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val result: Future[Result] = await(controller.submit()(subscriptionRequestWithName))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "backUrl" should {
    "go to the your income sources page" when {
      "in edit mode" in withController { controller =>
        controller.backUrl(true) mustBe controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      }
    }

    "go to the property accounting method page" when {
      "not in edit mode" in withController { controller =>
        controller.backUrl(false) mustBe controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyAccountingMethodController.show().url
      }
    }
  }

  object TestOverseasPropertyCheckYourAnswersController extends OverseasPropertyCheckYourAnswersController(
    mock[OverseasPropertyCheckYourAnswers],
    mockSubscriptionDetailsService,
    mockClientDetailsRetrieval,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  private def withController(testCode: OverseasPropertyCheckYourAnswersController => Any): Unit = {
    val mockView = mock[OverseasPropertyCheckYourAnswers]

    when(mockView(any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new OverseasPropertyCheckYourAnswersController(
      mockView,
      mockSubscriptionDetailsService,
      mockClientDetailsRetrieval,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig
    )

    testCode(controller)
  }
}
