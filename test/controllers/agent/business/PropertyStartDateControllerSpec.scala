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
import config.featureswitch.FeatureSwitching
import controllers.agent.AgentControllerBaseSpec
import forms.agent.PropertyStartDateForm
import models.DateModel
import models.common.PropertyModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.TestModels.testFullPropertyModel
import views.html.agent.business.PropertyStartDate

import java.time.LocalDate
import scala.concurrent.Future

class PropertyStartDateControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService with MockAuditingService  with FeatureSwitching{

  override def beforeEach(): Unit = {
    disable(featureSwitch = EnableTaskListRedesign)
    super.beforeEach()
  }

  override val controllerName: String = "PropertyStartDateController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestPropertyStartDateController$.show(isEditMode = false),
    "submit" -> TestPropertyStartDateController$.submit(isEditMode = false)
  )

  object TestPropertyStartDateController$ extends PropertyStartDateController(
    mock[PropertyStartDate],
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockLanguageUtils
  )

  "show" should {
    "throw an InternalServerException" when {
      "there are no client details in session" in withController { controller =>
        mockFetchProperty(None)

        intercept[InternalServerException](await(controller.show(isEditMode = false)(subscriptionRequest)))
          .message mustBe "[IncomeTaxAgentUser][clientDetails] - could not retrieve client details from session"
      }
    }

    "display the property start date view and return OK (200)" in withController { controller =>
      lazy val result: Result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

      mockFetchProperty(None)

      status(result) must be(Status.OK)
    }
  }

  "submit" when {
    val testValidMaxStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))

    val testPropertyStartDateModel: DateModel = testValidMaxStartDate

    def callSubmit(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequestWithName.post(
          PropertyStartDateForm.propertyStartDateForm(LocalDate.now(), LocalDate.now(), d => d.toString),
          testPropertyStartDateModel
        )
      )

    def callSubmitWithErrorForm(controller: PropertyStartDateController, isEditMode: Boolean): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequestWithName
      )

    "in edit mode" should  {
      "redirect to the uk property check your answers page" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(Some(testFullPropertyModel))

        val goodRequest = callSubmit(controller, isEditMode = true)

        status(goodRequest) must be(Status.SEE_OTHER)
        await(goodRequest)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url)

        verifyPropertySave(Some(testFullPropertyModel.copy(startDate = Some(testValidMaxStartDate), confirmed = false)))
      }
    }

    "redirect to agent uk property check your answers page" when {
      "in edit mode" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchProperty(Some(testFullPropertyModel))
        val goodRequest = callSubmit(controller, isEditMode = true)
        await(goodRequest)
        redirectLocation(goodRequest) mustBe Some(controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url)

        verifyPropertySave(Some(testFullPropertyModel.copy(startDate = Some(testValidMaxStartDate), confirmed = false)))
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in withController { controller =>
        val badRequest = callSubmitWithErrorForm(controller, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
      }
    }

    "throw an exception" when {
      "cannot save the start date" in withController { controller =>
        setupMockSubscriptionDetailsSaveFunctionsFailure()
        mockFetchProperty(None)

        val goodRequest: Future[Result] = callSubmit(controller, isEditMode = false)

        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is not in edit mode" should {
      "redirect back to agent what income source page" in withController { controller =>
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      }
    }


    "The back url is in edit mode" should {
      "redirect back to agent uk property check your answers page" in withController { controller =>
        controller.backUrl(isEditMode = true) mustBe
          controllers.agent.business.routes.PropertyCheckYourAnswersController.show(true).url
      }
    }
    "Not in Edit mode the back url with feature switch enabled" should{
      "redirect to new client's income sources" in withController{ controller =>
        enable(featureSwitch = EnableTaskListRedesign)
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.routes.YourIncomeSourceToSignUpController.show.url
      }
    }
    "Not in Edit mode the back url with feature switch disabled" should {
      "redirect to current client's income sources" in withController(controller =>
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.routes.WhatIncomeSourceToSignUpController.show().url
      )
    }

  }

  private def withController(testCode: PropertyStartDateController => Any): Unit = {
    val mockView = mock[PropertyStartDate]

    when(mockView(any(), any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new PropertyStartDateController(
      mockView,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockLanguageUtils
    )

    testCode(controller)
  }
}
