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

import connectors.IncomeTaxSubscriptionConnector
import controllers.agent.AgentControllerBaseSpec
import forms.agent.RemoveBusinessForm
import models.common.business._
import models.{DateModel, No, Yes}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers.{HTML, await, charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.mocks.{MockAuditingService, MockIncomeTaxSubscriptionConnector, MockRemoveBusinessService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.BusinessesKey
import views.html.agent.tasklist.selfemployment.RemoveSelfEmploymentBusiness

import scala.concurrent.Future

class RemoveSelfEmploymentBusinessControllerSpec extends AgentControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockRemoveBusinessService {

  override val controllerName: String = "RemoveSelfEmploymentBusinessController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  override val mockIncomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]

  private val encryptedTestBusinesses = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name").encrypt(crypto.QueryParameterCrypto)),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))).encrypt(crypto.QueryParameterCrypto))
    )
  )

  private val decryptedTestBusinesses = Seq(
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
      mockFetchAllSelfEmployments(encryptedTestBusinesses)

      val result: Future[Result] = await(controller.show("id")(subscriptionRequest))

      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }

    "throw an exception" when {
      "the Sole trader business cannot be retrieved" in withController { controller =>
        mockFetchAllSelfEmployments(encryptedTestBusinesses)

        val result: Future[Result] = await(controller.show("unknown")(subscriptionRequest))
        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "submit" should {
    "redirect to the task list page" when {
      "the user selects 'yes'" in withController { controller =>
        // get the businesses
        mockFetchAllSelfEmployments(encryptedTestBusinesses)
        // save the businesses with one removed.
        mockDeleteBusiness(Right("dummy"))

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), Yes)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)
        verifyDeleteBusiness(businessId = "id", decryptedTestBusinesses)
      }

      "the user selects 'no'" in withController { controller =>
        mockFetchAllSelfEmployments(encryptedTestBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), No)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.tasklist.routes.TaskListController.show().url)
        verifySelfEmploymentsSave[Seq[SelfEmploymentData]](BusinessesKey, None)
      }
    }

    "throw an exception" when {
      "the user submits invalid data" in withController { controller =>
        mockFetchAllSelfEmployments(encryptedTestBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest
        ))

        status(result) must be(Status.BAD_REQUEST)
        verifySelfEmploymentsSave[Seq[SelfEmploymentData]](BusinessesKey, None)
      }
    }
  }

  private def withController(testCode: RemoveSelfEmploymentBusinessController => Any): Unit = {
    val view = mock[RemoveSelfEmploymentBusiness]

    when(view(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new RemoveSelfEmploymentBusinessController(
      view,
      mockAuditingService,
      mockAuthService,
      MockSubscriptionDetailsService,
      mockRemoveBusinessService,
      mockIncomeTaxSubscriptionConnector
    )

    testCode(controller)
  }
}