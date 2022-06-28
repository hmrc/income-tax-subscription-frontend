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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import controllers.ControllerBaseSpec
import forms.individual.business.RemoveBusinessForm
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
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockRemoveBusinessService, MockSubscriptionDetailsService}
import utilities.SubscriptionDataKeys.BusinessesKey
import views.html.individual.incometax.business.RemoveBusiness

import scala.concurrent.Future

class RemoveBusinessControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with MockSubscriptionDetailsService
  with MockIncomeTaxSubscriptionConnector
  with MockRemoveBusinessService {

  override val controllerName: String = "RemoveBusinessController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()
  override val mockIncomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]

  private val testBusinesses = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel("123", Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )
  )

  "show" should {
    "return OK status" when {
      "the save and retrieve feature enabled" in withController { controller =>
        enable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.show("id")(subscriptionRequest))

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
        charset(result) mustBe Some(Codec.utf_8.charset)
      }
    }

    "throw an exception" when {
      "the save and retrieve feature disabled" in withController { controller =>
        disable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.show("id")(subscriptionRequest))
        result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
      }

      "the Sole trader business cannot be retrieved" in withController { controller =>
        enable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.show("unknown")(subscriptionRequest))
        result.failed.futureValue mustBe an[uk.gov.hmrc.http.InternalServerException]
      }
    }
  }

  "submit" should {
    "redirect to the task list page" when {
      "the user selects 'yes'" in withController { controller =>
        enable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), Yes)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.business.routes.TaskListController.show().url)
        verifyDeleteBusiness(businessId = "id", testBusinesses)
      }

      "the user selects 'no'" in withController { controller =>
        enable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), No)
        ))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.business.routes.TaskListController.show().url)
        verifySelfEmploymentsSave[Seq[SelfEmploymentData]](BusinessesKey, None)
      }
    }

    "throw an exception" when {
      "the save and retrieve feature not enabled" in withController { controller =>
        disable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest.post(RemoveBusinessForm.removeBusinessForm(), No)
        ))

        result.failed.futureValue mustBe an[uk.gov.hmrc.http.NotFoundException]
        verifySelfEmploymentsSave[Seq[SelfEmploymentData]](BusinessesKey, None)
      }

      "the user submits invalid data" in withController { controller =>
        enable(SaveAndRetrieve)
        mockGetSelfEmploymentsSeq[SelfEmploymentData](BusinessesKey)(testBusinesses)

        val result: Future[Result] = await(controller.submit("id")(
          subscriptionRequest
        ))

        status(result) must be(Status.BAD_REQUEST)
        verifySelfEmploymentsSave[Seq[SelfEmploymentData]](BusinessesKey, None)
      }
    }
  }

  private def withController(testCode: RemoveBusinessController => Any): Unit = {
    val view = mock[RemoveBusiness]

    when(view(any(), any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(HtmlFormat.empty)

    val controller = new RemoveBusinessController(
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
