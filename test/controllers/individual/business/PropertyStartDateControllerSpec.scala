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

package controllers.individual.business

import agent.audit.mocks.MockAuditingService
import controllers.ControllerBaseSpec
import forms.individual.business.PropertyStartDateForm
import models.DateModel
import models.common.PropertyModel
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.mocks.MockSubscriptionDetailsService
import utilities.SubscriptionDataKeys.PropertyStartDate
import utilities.TestModels.testFullPropertyModel
import views.individual.mocks.MockPropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class PropertyStartDateControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockPropertyStartDate
   {

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController.submit(isEditMode = false)
  )

  object TestPropertyStartDateController extends PropertyStartDateController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils,
    propertyStartDate
  )

  trait Test {
    val controller = new PropertyStartDateController(
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils,
      propertyStartDate
    )
  }

  "show" should {
    "display the property accounting method view and return OK (200)" in new Test {
      mockPropertyStartDate()

      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequest))
      mockFetchProperty(None)

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(PropertyStartDate, 0)
    }
  }

  "submit" should {

    val maxDate = LocalDate.now.minusYears(1)
    val testValidMaxDate: DateModel = DateModel.dateConvert(maxDate)
    val minDate = LocalDate.of(1900, 1, 1)

    val testPropertyStartDateModel: DateModel = testValidMaxDate

    def callSubmit(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(PropertyStartDateForm.propertyStartDateForm(minDate, maxDate, d => d.toString), testPropertyStartDateModel)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = TestPropertyStartDateController.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "redirect to uk property accounting method page" when {
      "not in edit mode" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(None)

        val goodRequest = callSubmit(isEditMode = false)

        status(goodRequest) must be(Status.SEE_OTHER)
        await(goodRequest)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyAccountingMethodController.show().url)

        verifyPropertySave(Some(PropertyModel(startDate = Some(testValidMaxDate))))

      }
    }

    "redirect to uk property check your answers page" when {
      "in edit mode" in {
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(Some(testFullPropertyModel))
        val goodRequest = callSubmit(isEditMode = true)
        await(goodRequest)
        redirectLocation(goodRequest) mustBe Some(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(true).url)

        verifyPropertySave(Some(testFullPropertyModel.copy(startDate = Some(testValidMaxDate), confirmed = false)))
      }
    }

    "return BAD_REQUEST" when {
      "there is an invalid submission with an error form" in {
        mockPropertyStartDate()

        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyPropertySave(None)
        verifySubscriptionDetailsFetchAll(Some(0))
      }
    }

    "throw an exception" when {
      "cannot save the start date" in {
        setupMockSubscriptionDetailsSaveFunctionsFailure()
        mockFetchProperty(None)

        val goodRequest: Future[Result] = callSubmit(isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is in edit mode" should {
      "redirect back to uk property check your answers page" in new Test {
        controller.backUrl(isEditMode = true) mustBe
          controllers.individual.business.routes.PropertyCheckYourAnswersController.show(true).url
      }
    }

    "The back url is not in edit mode" should {
      "redirect back to what income source page" in new Test {
        controller.backUrl(isEditMode = false) mustBe
          controllers.individual.incomesource.routes.WhatIncomeSourceToSignUpController.show().url
      }
    }
  }
}

