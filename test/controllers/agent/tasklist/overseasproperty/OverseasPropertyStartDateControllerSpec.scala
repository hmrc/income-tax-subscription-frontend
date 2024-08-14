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

import config.featureswitch.FeatureSwitching
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import forms.agent.OverseasPropertyStartDateForm
import models.DateModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockAgentAuthService
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockReferenceRetrieval, MockSubscriptionDetailsService}
import utilities.TestModels.testPropertyStartDateModel
import views.agent.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAgentAuthService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval
  with MockOverseasPropertyStartDate
  with FeatureSwitching {

  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController$.submit(isEditMode = false)
  )

  trait Test {
    val controller = new OverseasPropertyStartDateController(
      mockOverseasPropertyStartDate,
      mockSubscriptionDetailsService,
      mockClientDetailsRetrieval,
      mockReferenceRetrieval
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      mockLanguageUtils
    )
  }

  object TestOverseasPropertyStartDateController$ extends OverseasPropertyStartDateController(
    mockOverseasPropertyStartDate,
    mockSubscriptionDetailsService,
    mockClientDetailsRetrieval,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig,
    mockLanguageUtils
  )

  "show" should {
    "display the foreign property start date view and return OK (200) without fetching income source" in new Test {
      mockFetchOverseasPropertyStartDate(Some(testPropertyStartDateModel.startDate))

      val result: Future[Result] = controller.show(isEditMode = false)(subscriptionRequestWithName)

      status(result) must be(Status.OK)
      contentType(result) mustBe Some(HTML)
    }
  }

  "submit" should {

    val maxStartDate = LocalDate.now.minusYears(1)
    val testValidMaxStartDate: DateModel = DateModel.dateConvert(maxStartDate)
    val minStartDate = LocalDate.of(1900, 1, 1)

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequestWithName.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(minStartDate, maxStartDate, d => d.toString),
        testValidMaxStartDate)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequestWithName
    )

    "redirect to agent foreign property accounting method page" when {
      "not in edit mode and the feature switch is disabled" in {
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
      }
    }

    "redirect to agent overseas check your answers page" when {
      "in edit mode" in {
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(Right(PostSubscriptionDetailsSuccessResponse))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(true).url)

        await(goodRequest)
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in {
        mockSaveOverseasPropertyStartDate(testValidMaxStartDate)(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val goodRequest = callSubmit(isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is not in edit mode" when {
      "redirect to income source page" in new Test {
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      }
    }

    "The back url is in edit mode" when {
      "the user click back url" should {
        "redirect to agent overseas property check your answer page" in new Test {
          controller.backUrl(isEditMode = true) mustBe
            routes.OverseasPropertyCheckYourAnswersController.show(true).url
        }
      }
    }
  }
}
