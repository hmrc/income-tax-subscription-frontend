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

import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import connectors.subscriptiondata.mocks.MockIncomeTaxSubscriptionConnector
import controllers.ControllerBaseSpec
import forms.individual.business.AddAnotherBusinessForm
import forms.submapping.YesNoMapping
import models.DateModel
import models.common.BusinessNameModel
import models.individual.business.{Address, BusinessAddressModel, BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import utilities.individual.ImplicitDateFormatterImpl

import scala.concurrent.Future

class SelfEmploymentsCYAControllerSpec extends ControllerBaseSpec with MockIncomeTaxSubscriptionConnector with FeatureSwitching {

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)

  val id: String = "testId"

  override val controllerName: String = "SelfEmploymentsCYAController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestSelfEmploymentsCYAController.show()
  )

  object TestSelfEmploymentsCYAController extends SelfEmploymentsCYAController(
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

    "return OK (200)" when {
      "the connector returns successful json" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData
        )))

        val result: Future[Result] = TestSelfEmploymentsCYAController.show()(FakeRequest())
        status(result) mustBe OK
        contentType(result) mustBe Some("text/html")
      }
    }

    "return (303) redirect to Income source page" when {
      "no businesses are returned" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(None)

        val result: Future[Result] = TestSelfEmploymentsCYAController.show()(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
      }
      "no complete businesses are returned" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData.copy(businessName = None)
        )))

        val result: Future[Result] = TestSelfEmploymentsCYAController.show()(FakeRequest())
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
      }
    }

    "throw an internal server error" when {
      "there is an unexpected status failure" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetailsException[Seq[SelfEmploymentData]]("Businesses")

        val response = intercept[InternalServerException](await(TestSelfEmploymentsCYAController.show()(FakeRequest())))
        response.message mustBe "[SelfEmploymentsCYAController][show] - getSelfEmployments connection failure, error: exception"
      }
    }

  }

  "submit" should {
    "return 303, SEE_OTHER" when {
      "the connector returns successful json and submit with Yes option redirect to Initialise controller" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData
        )))

        val result: Future[Result] = TestSelfEmploymentsCYAController.submit()(FakeRequest()
          .withFormUrlEncodedBody(AddAnotherBusinessForm.addAnotherBusiness -> YesNoMapping.option_yes))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.business.routes.InitialiseController.initialise().url)

      }

      "the connector returns successful json and submit with No option redirect to Business Accounting method page" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData
        )))

        val result: Future[Result] = TestSelfEmploymentsCYAController.submit()(FakeRequest()
          .withFormUrlEncodedBody(AddAnotherBusinessForm.addAnotherBusiness -> YesNoMapping.option_no))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.individual.business.routes.BusinessAccountingMethodController.show().url)
      }

    }

    "return (303) redirect to business start date page" when {
      "no businesses are returned" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(None)

        val result: Future[Result] = TestSelfEmploymentsCYAController.submit()(FakeRequest()
          .withFormUrlEncodedBody(AddAnotherBusinessForm.addAnotherBusiness -> YesNoMapping.option_no))
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
      }
      "no complete businesses are returned" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetails[Seq[SelfEmploymentData]]("Businesses")(Some(Seq(
          businessData.copy(businessName = None)
        )))

        val result: Future[Result] = TestSelfEmploymentsCYAController.submit()(FakeRequest()
          .withFormUrlEncodedBody(AddAnotherBusinessForm.addAnotherBusiness -> YesNoMapping.option_no))
        status(result) mustBe 303
        redirectLocation(result) mustBe Some(controllers.individual.incomesource.routes.IncomeSourceController.show().url)
      }
    }

    "throw an internal server error" when {
      "there is an unexpected status failure" in {
        enable(ReleaseFour)
        mockAuthSuccess()
        mockGetSubscriptionDetailsException[Seq[SelfEmploymentData]]("Businesses")

        val response = intercept[InternalServerException](await(TestSelfEmploymentsCYAController.submit()(FakeRequest())))
        response.message mustBe "[SelfEmploymentsCYAController][submit] - getSelfEmployments connection failure, error: exception"
      }
    }

  }

  authorisationTests()

}
