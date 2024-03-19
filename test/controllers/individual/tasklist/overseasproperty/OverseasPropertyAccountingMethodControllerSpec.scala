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

package controllers.individual.tasklist.overseasproperty

import config.featureswitch.FeatureSwitch.EnableTaskListRedesign
import controllers.individual.ControllerBaseSpec
import forms.individual.business.AccountingMethodOverseasPropertyForm
import models.Cash
import models.common.OverseasPropertyModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockSessionDataService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.OverseasPropertyAccountingMethod
import views.html.individual.tasklist.overseasproperty.OverseasPropertyAccountingMethod

import scala.concurrent.Future

class OverseasPropertyAccountingMethodControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockSessionDataService
  with MockAuditingService {

  override def beforeEach(): Unit = {
    disable(EnableTaskListRedesign)
    super.beforeEach()
  }

  override val controllerName: String = "ForeignPropertyAccountingMethod"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: OverseasPropertyAccountingMethodController => Any): Unit = {
    val overseasPropertyAccountingMethodView = mock[OverseasPropertyAccountingMethod]

    when(overseasPropertyAccountingMethodView(any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new OverseasPropertyAccountingMethodController(
      overseasPropertyAccountingMethodView
    )(
      mockAuditingService,
      mockAuthService,
      mockSessionDataService,
      appConfig,
      MockSubscriptionDetailsService
    )

    testCode(controller)
  }

  "show" should {
    "display the foreign property accounting method view and return OK (200)" in withController { controller =>
      lazy val result = await(controller.show(isEditMode = false)(subscriptionRequest))

      mockFetchOverseasProperty(None)

      status(result) must be(Status.OK)
      verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
      verifySubscriptionDetailsFetchAll(Some(1))
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest.post(AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm, Cash)
    )

    def callSubmitWithErrorForm(isEditMode: Boolean): Future[Result] = controller.submit(isEditMode = isEditMode)(
      subscriptionRequest
    )

    "redirect to overseas property check your answer page" when {
      "not in edit mode" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = false)

        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
      }

      "in edit mode" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctions()

        val goodRequest = callSubmit(isEditMode = true)

        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show(true).url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))
      }
    }

    "return bad request status (400)" when {
      "there is an invalid submission with an error form" in {
        val badRequest = callSubmitWithErrorForm(isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifySubscriptionDetailsSave(OverseasPropertyAccountingMethod, 0)
        verifySubscriptionDetailsFetchAll(Some(0))
      }
    }

    "throw an exception" when {
      "cannot save the accounting method" in {
        mockFetchOverseasProperty(Some(OverseasPropertyModel()))
        setupMockSubscriptionDetailsSaveFunctionsFailure()

        val goodRequest = callSubmit(isEditMode = false)
        goodRequest.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }

    "The back url is in edit mode" should {
      "redirect to overseas property start date page" in withController { controller =>
        controller.backUrl(isEditMode = true) mustBe
          routes.OverseasPropertyCheckYourAnswersController.show(true).url
      }
    }

    "The back url" should {
      "is not in edit mode" when {
        "redirect back to overseas property start date page" in withController { controller =>
          controller.backUrl(isEditMode = false) mustBe
            routes.OverseasPropertyStartDateController.show().url
        }
      }
    }
  }
}