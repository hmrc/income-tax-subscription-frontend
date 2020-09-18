/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.subscription

import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import models.DateModel
import models.common.BusinessNameModel
import models.individual.business._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.BusinessesKey

import scala.concurrent.Future

class RemoveBusinessControllerSpec extends ControllerBaseSpec with MockIncomeTaxSubscriptionConnector {

  val id: String = "testId"

  override val controllerName: String = "RemoveBusinessController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestRemoveBusinessController.show(id)
  )

  object TestRemoveBusinessController extends RemoveBusinessController(
    mockAuthService,
    mockIncomeTaxSubscriptionConnector,
    mockMessagesControllerComponents
  )

  val businessData: SelfEmploymentData = SelfEmploymentData(
    id = id,
    businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "2017"))),
    businessName = Some(BusinessNameModel("ABC Limited")),
    businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
    businessAddress = Some(BusinessAddressModel("12345", Address(Seq("line1"), "TF3 4NT")))
  )

  "show" should {
    "return (303) redirect to business CYA page when Remove business has multiple business and click remove business" when {
      "the connector returns successful json" in {
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData.copy(id = "testId2")
        )))
        mockSaveSubscriptionDetails[Seq[SelfEmploymentData]](
          id = BusinessesKey,
          value = Seq(businessData.copy(id = "testId2"))
        )(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = TestRemoveBusinessController.show(id)(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.SelfEmploymentsCYAController.show().url)
      }
    }

    "return (303) redirect to how-do-you-receive-your-income page when Remove business has one business and click remove business" when {
      "the connector returns successful json" in {
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData
        )))
        mockSaveSubscriptionDetails[Seq[SelfEmploymentData]](
          id = BusinessesKey,
          value = Seq()
        )(Right(PostSubscriptionDetailsSuccessResponse))

        val result: Future[Result] = TestRemoveBusinessController.show(id)(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result).get must include(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
      }
    }

    "return (303) redirect to business start date page" when {
      "no businesses are returned" in {
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(None)

        val result: Future[Result] = TestRemoveBusinessController.show(id)(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result).get must include(controllers.individual.business.routes.InitialiseController.initialise().url)
      }
      "no complete businesses are returned" in {
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData.copy(businessName = None)
        )))

        val result: Future[Result] = TestRemoveBusinessController.show(id)(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.business.routes.InitialiseController.initialise().url)
      }
    }

    "throw an internal server error" when {
      "there is an unexpected status failure when mockGetSelfEmployments returns error" in {
        mockAuthSuccess()
        mockGetSubscriptionDetailsException[Seq[SelfEmploymentData]]("Businesses")

        val response = intercept[InternalServerException](await(TestRemoveBusinessController.show(id)(FakeRequest())))
        response.message mustBe "[RemoveBusinessController][show] - incomeTaxSubscriptionConnector connection failed, error: exception"
      }

      "there is an unexpected status failure when mockSaveSelfEmployments returns error" in {
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData
        )))
        mockSaveSubscriptionDetails[Seq[SelfEmploymentData]](
          id = BusinessesKey,
          value = Seq()
        )(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

        val response = intercept[InternalServerException](await(TestRemoveBusinessController.show(id)(FakeRequest())))
        response.message mustBe "[RemoveBusinessController][show] - incomeTaxSubscriptionConnector connection failed, error: saveSelfEmployments failure, status: 500"
      }
    }

  }

  authorisationTests()

}
