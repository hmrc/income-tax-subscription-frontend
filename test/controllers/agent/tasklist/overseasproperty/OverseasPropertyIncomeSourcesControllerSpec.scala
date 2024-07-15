/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.agent.AgentControllerBaseSpec
import forms.agent.IncomeSourcesOverseasPropertyForm
import models.common.OverseasPropertyModel
import models.{AccountingMethod, Cash, DateModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{POST, await, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockClientDetailsRetrieval, MockReferenceRetrieval, MockSubscriptionDetailsService}
import views.html.agent.tasklist.overseasproperty.IncomeSourcesOverseasProperty

import scala.concurrent.Future

class OverseasPropertyIncomeSourcesControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockAuditingService
  with MockReferenceRetrieval
  with MockClientDetailsRetrieval {

  override val controllerName: String = "OverseasPropertyIncomeSources"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private def withController(testCode: IncomeSourcesOverseasPropertyController => Any): Unit = {
    val overseasPropertyIncomeSourcesView = mock[IncomeSourcesOverseasProperty]

    when(overseasPropertyIncomeSourcesView.apply(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new IncomeSourcesOverseasPropertyController(
      overseasPropertyIncomeSourcesView,
      mockReferenceRetrieval,
      mockClientDetailsRetrieval,
      MockSubscriptionDetailsService,
    )(
      mockAuditingService,
      mockAuthService,
      appConfig,
      mockLanguageUtils
    )

    testCode(controller)
  }

  "show" when {
    "there is no stored data" should {
      "display the overseas property income sources view with empty form and return OK (200)" in withController { controller =>
        mockFetchOverseasProperty(None)

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }

    "there is stored start date only" should {
      "display the overseas property income sources view with start date pre-filled and return OK (200)" in withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel(startDate = Some(DateModel("10", "10", "2020")))))

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }

    "there is stored accounting method only" should {
      "display the overseas property income sources view with accounting method pre-filled and return OK (200)" in withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel(accountingMethod = Some(Cash))))

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }

    "there is full data stored" should {
      "display the overseas property income sources view with full data pre-filled and return OK (200)" in withController { controller =>
        mockFetchOverseasProperty(Some(OverseasPropertyModel(
          startDate = Some(DateModel("10", "10", "2020")),
          accountingMethod = Some(Cash)
        )))

        val result = await(controller.show(isEditMode = false)(subscriptionRequestWithName))

        status(result) must be(Status.OK)
      }
    }
  }

  "submit" should withController { controller =>

    def callSubmit(isEditMode: Boolean, maybeStartDate: Option[DateModel], maybeAccountingMethod: Option[AccountingMethod]): Future[Result] =
      controller.submit(isEditMode = isEditMode)(
        subscriptionRequestWithName.withFormUrlEncodedBody(IncomeSourcesOverseasPropertyForm.createOverseasPropertyMapData(maybeStartDate, maybeAccountingMethod).toSeq: _*)
          .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
          .withMethod(POST)
      )

    "return bad request (400)" when {
      "nothing is submitted" in {
        val badRequest = callSubmit(maybeStartDate = None, maybeAccountingMethod = None, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyOverseasPropertySave(None)
      }

      "only start date is submitted" in {
        val badRequest = callSubmit(maybeStartDate = Some(DateModel("10", "10", "2020")), maybeAccountingMethod = None, isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyOverseasPropertySave(None)
      }

      "only accounting method is submitted" in {
        val badRequest = callSubmit(maybeStartDate = None, maybeAccountingMethod = Some(Cash), isEditMode = false)

        status(badRequest) must be(Status.BAD_REQUEST)

        await(badRequest)
        verifyOverseasPropertySave(None)
      }
    }

    "redirect to check your answers page" when {
      "full data is submitted" in {
        mockFetchOverseasProperty(None)
        setupMockSubscriptionDetailsSaveFunctions()
        mockDeleteIncomeSourceConfirmationSuccess()

        val goodRequest = callSubmit(isEditMode = false,
          maybeStartDate = Some(DateModel("10", "10", "2020")), maybeAccountingMethod = Some(Cash),
        )

        status(goodRequest) must be(Status.SEE_OTHER)
        redirectLocation(goodRequest) mustBe Some(routes.OverseasPropertyCheckYourAnswersController.show().url)

        await(goodRequest)
        verifyOverseasPropertySave(Some(OverseasPropertyModel(startDate = Some(DateModel("10", "10", "2020")), accountingMethod = Some(Cash))))
      }
    }
  }

  "The back url" when {
    "in edit mode" should {
      "redirect to the agent overseas property check your answers" in withController { controller =>
        controller.backUrl(
          isEditMode = true
        ) mustBe routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url
      }

    }

    "not in edit mode" should {
      "redirect back to client income sources page" in withController { controller =>
        controller.backUrl(isEditMode = false) mustBe
          controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      }
    }
  }
}