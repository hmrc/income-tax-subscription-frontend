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

import agent.audit.mocks.{MockAuditingService, MockOverseasPropertyStartDate}
import controllers.agent.AgentControllerBaseSpec
import forms.agent.OverseasPropertyStartDateForm
import models.DateModel
import models.common.OverseasPropertyModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.agent.mocks.MockAgentAuthService
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.OverseasPropertyStartDate
import utilities.TestModels.{testAccountingMethodProperty, testPropertyStartDateModel}

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAgentAuthService with MockAuditingService with MockOverseasPropertyStartDate {
  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController$.submit(isEditMode = false)
  )

  trait Test {
    val controller = new OverseasPropertyStartDateController(
      mockAuditingService,
      mockOverseasPropertyStartDate,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils
    )
  }

  object TestOverseasPropertyStartDateController$ extends OverseasPropertyStartDateController(
    mockAuditingService,
    mockOverseasPropertyStartDate,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  "show" should {
    "display the foreign property start date view and return OK (200) without fetching income source" in new Test {
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchOverseasProperty(Some(OverseasPropertyModel(
        accountingMethod = Some(testAccountingMethodProperty.propertyAccountingMethod),
        startDate = Some(testPropertyStartDateModel.startDate)
      )))

      status(result) must be(Status.OK)
      verifyOverseasPropertySave(None)
    }
  }

  "submit" should {

    val maxStartDate = LocalDate.now.minusYears(1)
    val testValidMaxStartDate: DateModel = DateModel.dateConvert(maxStartDate)
    val minStartDate = LocalDate.of(1900, 1, 1)

    def callSubmit(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(minStartDate, maxStartDate, d => d.toString),
        testValidMaxStartDate)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestOverseasPropertyStartDateController$.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "redirect to agent foreign property accounting method page" when {
      "not in edit mode" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
      }
    }

    "redirect to agent overseas check your answers page" when {
      "in edit mode" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callSubmit(isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyStartDate, 0)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val goodRequest = callSubmit(isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is not in edit mode" when {
      "save and retrieve is enabled" when {
        "redirect to agent income source page" in new Test {
          controller.backUrl(isEditMode = false) mustBe
            controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
        }
      }
    }

    "The back url is in edit mode" when {
      "save and retrieve is enabled" when {
        "the user click back url" should {
          "redirect to agent overseas property check your answer page" in new Test {
            controller.backUrl(isEditMode = true) mustBe
              controllers.agent.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url
          }
        }
      }
    }
  }
}
