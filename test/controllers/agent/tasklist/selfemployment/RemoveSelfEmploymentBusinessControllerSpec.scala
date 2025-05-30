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

package controllers.agent.tasklist.selfemployment

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import controllers.agent.AgentControllerBaseSpec
import controllers.agent.actions.mocks.{MockConfirmedClientJourneyRefiner, MockIdentifierAction}
import forms.agent.RemoveBusinessForm
import models.common.business._
import models.{DateModel, No, Yes}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks._
import views.html.agent.tasklist.selfemployment.RemoveSelfEmploymentBusiness

import scala.concurrent.Future

class RemoveSelfEmploymentBusinessControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockRemoveBusinessService
  with MockIdentifierAction
  with MockConfirmedClientJourneyRefiner {

  override val controllerName: String = "RemoveSelfEmploymentBusinessController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  private val testBusinesses = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )
  )

  "show" should {
    "return OK status" in withController { controller =>
      mockFetchAllSelfEmployments(testBusinesses)

      val result: Future[Result] = await(controller.show("id")(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "redirect to Business Already Removed" when {
      "the Sole trader business cannot be retrieved" in withController { controller =>
        mockFetchAllSelfEmployments(testBusinesses)

        val result: Future[Result] = await(controller.show("unknown")(subscriptionRequest))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url)
      }
    }
  }

  "submit" should {
    "redirect to the your income sources page" when {
      "the user selects 'yes'" in withController { controller =>
        mockFetchAllSelfEmployments(testBusinesses)
        mockDeleteBusiness(Right(DeleteSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), Yes)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
        verifyDeleteBusiness(businessId = "id")
      }

      "the user selects 'no'" in withController { controller =>
        mockFetchAllSelfEmployments(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), No)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url)
      }
    }

    "return BAD_REQUEST" when {
      "the user submits invalid data" in withController { controller =>
        mockFetchAllSelfEmployments(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest
        ))

        status(result) must be(Status.BAD_REQUEST)
      }
    }
  }

  private def withController(testCode: RemoveSelfEmploymentBusinessController => Any): Unit = {
    val view = mock[RemoveSelfEmploymentBusiness]

    when(view(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new RemoveSelfEmploymentBusinessController(
      view,
      fakeIdentifierAction,
      fakeConfirmedClientJourneyRefiner,
      mockSubscriptionDetailsService,
      mockRemoveBusinessService
    )

    testCode(controller)
  }
}
