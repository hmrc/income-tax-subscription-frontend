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

package controllers.individual.business

import controllers.ControllerBaseSpec
import forms.individual.business.OverseasPropertyStartDateForm
import models.DateModel
import models.common.OverseasPropertyModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockAuthService
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import views.individual.mocks.MockOverseasPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class OverseasPropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuthService with MockAuditingService  with MockOverseasPropertyStartDate {

  override val controllerName: String = "OverseasPropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestOverseasPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestOverseasPropertyStartDateController.submit(isEditMode = false)
  )

  object TestOverseasPropertyStartDateController extends OverseasPropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils,
    overseasPropertyStartDate
  )

  "show" should {
    "display the foreign property start date view and return OK (200)" in withController { controller =>
      mockOverseasPropertyStartDateView()
      mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))

      status(result) must be(Status.OK)
      verifySubscriptionDetailsFetchAll(Some(1))
    }
  }

  "submit" should {
    val testValidMaxStartDate: DateModel = DateModel.dateConvert( LocalDate.now.minusYears(1))

    def callPost(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest.post(OverseasPropertyStartDateForm.overseasPropertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testValidMaxStartDate)
      )

    def callPostWithErrorForm(controller: OverseasPropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequest
      )

    "When it is not in edit mode" should {
      "redirect to foreign property accounting method page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))

        val goodRequest = callPost(controller, isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyAccountingMethodController.show().url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
      }
    }

    "When it is in edit mode" should {
      "redirect to overseas property check your answers page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("22", "11", "2021")))))
        val goodRequest = callPost(controller, isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(testValidMaxStartDate))))
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in withController { controller =>
        mockOverseasPropertyStartDateView()

        val badRequest = callPostWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyOverseasPropertySave(None)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val goodRequest = callPost(controller, isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "backUrl" when {
      "in edit mode" should {
        "redirect to overseas property check your answers page" in withController { controller =>
          controller.backUrl(isEditMode = true) mustBe
            controllers.individual.business.routes.OverseasPropertyCheckYourAnswersController.show(true).url
        }
      }

      "not in edit mode" should {
        "redirect to what income source to sign up page" in withController { controller =>
          controller.backUrl(isEditMode = false) mustBe
            controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
        }
      }
    }
  }

  private def withController(testCode: OverseasPropertyStartDateController => Any) = {
    val controller = new OverseasPropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils,
      overseasPropertyStartDate
    )

    testCode(controller)
  }

}
