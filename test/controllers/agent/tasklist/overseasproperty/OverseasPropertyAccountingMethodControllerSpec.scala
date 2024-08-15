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
import controllers.agent.AgentControllerBaseSpec
import forms.agent.AccountingMethodOverseasPropertyForm
import models.Cash
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.agent.mocks.MockOverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval
  with MockOverseasPropertyAccountingMethod {

  override val controllerName: String = "OverseasPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyAccountingMethodController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyAccountingMethodController.submit(isEditMode = false)
  )

  object TestOverseasPropertyAccountingMethodController extends OverseasPropertyAccountingMethodController(
    overseasPropertyAccountingMethod,
    mockSubscriptionDetailsService,
    mockClientDetailsRetrieval,
    mockReferenceRetrieval
  )(
    mockAuditingService,
    mockAuthService,
    appConfig
  )

  "show" when {
    "display the overseas property accounting method view and return OK (200)" when {
      "there is no previously selected accounting method" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequestWithName))
        mockFetchOverseasPropertyAccountingMethod(None)
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
      }

      "there is a previously selected answer of CASH" in {
        lazy val result = await(TestOverseasPropertyAccountingMethodController.show(isEditMode = false)(subscriptionRequestWithName))

        mockFetchOverseasPropertyAccountingMethod(Some(Cash))
        mockOverseasPropertyAccountingMethod()

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should {
    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequestWithName.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyAccountingMethodController.submit(isEditMode)(
      subscriptionRequestWithName
    )

    "redirect to agent overseas property check your answers page" in {
      mockSaveOverseasAccountingMethodProperty(Cash)(
        Right(PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse)
      )

      val goodRequest = callSubmit(isEditMode = false)

      status(goodRequest) must be(Status.SEE_OTHER)

      redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)

      await(goodRequest)
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        mockOverseasPropertyAccountingMethod()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockSaveOverseasAccountingMethodProperty(Cash)(
          Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
        )

        val goodRequest = callSubmit(isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is in edit mode" should {
      "redirect to the agent overseas property check your answers page" in {
        TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = true) mustBe
          routes.OverseasPropertyCheckYourAnswersController.show(true).url
      }
    }

    "The back url is not in edit mode" should {
      "redirect to the Overseas Property Start Date page" in {
        TestOverseasPropertyAccountingMethodController.backUrl(isEditMode = false) mustBe
          routes.OverseasPropertyStartDateController.show().url
      }
    }
  }
}
