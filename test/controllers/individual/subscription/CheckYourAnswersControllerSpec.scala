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
import controllers.ControllerBaseSpec
import models.common.{AccountingMethodModel, IncomeSourceModel}
import models.individual.business.SelfEmploymentData
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import services.individual.mocks.MockSubscriptionOrchestrationService
import services.mocks.{MockIncomeTaxSubscriptionConnector, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.MtditId
import utilities.SubscriptionDataUtil._
import utilities.TestModels._
import utilities.ImplicitDateFormatterImpl
import utilities.individual.TestConstants._

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector
  with FeatureSwitching {

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    mockAuthService,
    MockSubscriptionDetailsService,
    mockSubscriptionOrchestrationService,
    mockIncomeTaxSubscriptionConnector,
    mockImplicitDateFormatter
  )

  "Calling the show action of the CheckYourAnswersController with an authorised user" when {

    def result: Future[Result] = TestCheckYourAnswersController.show(subscriptionRequest)

    "return ok (200) for business journey" in {
      val testBusinessCacheMap = testCacheMapCustom(
        incomeSource = testIncomeSourceBusiness
      )
      mockFetchAllFromSubscriptionDetails(testBusinessCacheMap)
      mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
      mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
      status(result) must be(Status.OK)
    }

    "return ok (200) for property only journey)" in {
      val testPropertyCacheMap = testCacheMap(
        incomeSource = testIncomeSourceProperty
      )
      mockFetchAllFromSubscriptionDetails(testPropertyCacheMap)
      mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
      mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)

      status(result) must be(Status.OK)
    }

    "return ok (200) for both journey" in {
      mockFetchAllFromSubscriptionDetails(testCacheMap)
      mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
      mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)

      status(result) must be(Status.OK)
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" should {

    lazy val request = subscriptionRequest

    def call: Future[Result] = TestCheckYourAnswersController.submit(request)

    "When the submission is successful and release four is disabled" should {
      lazy val result = call

      "return a redirect status (SEE_OTHER - 303)" in {
        disable(ReleaseFour)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap)
        mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
        mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
        mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary(), isReleaseFourEnabled = false, isPropertyNextTaxYearEnabled = false)
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifySubscriptionDetailsSave(MtditId, 1)
        verifySubscriptionDetailsFetchAll(2)
      }

      s"redirect to '${controllers.individual.subscription.routes.ConfirmationController.show().url}'" in {
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
      }
    }

    "When the submission is successful and release four is enabled" should {
      lazy val result = call

      "return a redirect status (SEE_OTHER - 303)" in {
        enable(ReleaseFour)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap)
        mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
        mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
        mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary(), isReleaseFourEnabled = true, isPropertyNextTaxYearEnabled = false)
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifySubscriptionDetailsSave(MtditId, 1)
        verifySubscriptionDetailsFetchAll(2)
      }

      s"redirect to '${controllers.individual.subscription.routes.ConfirmationController.show().url}'" in {
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
      }
    }

    "When the submission is successful and release four + property next tax year is enabled" should {
      lazy val result = call

      "return a redirect status (SEE_OTHER - 303)" in {
        enable(ReleaseFour)
        setupMockSubscriptionDetailsSaveFunctions()
        mockFetchAllFromSubscriptionDetails(testCacheMap)
        mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
        mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
        mockCreateSubscriptionSuccess(testNino, testCacheMap.getSummary(), isReleaseFourEnabled = true, isPropertyNextTaxYearEnabled = true)
        status(result) must be(Status.SEE_OTHER)
        await(result)
        verifySubscriptionDetailsSave(MtditId, 1)
        verifySubscriptionDetailsFetchAll(2)
      }

      s"redirect to '${controllers.individual.subscription.routes.ConfirmationController.show().url}'" in {
        redirectLocation(result) mustBe Some(controllers.individual.subscription.routes.ConfirmationController.show().url)
      }
    }

    "When the submission is unsuccessful" should {
      lazy val result = call

      "return a internalServer error" in {
        disable(ReleaseFour)
        mockFetchAllFromSubscriptionDetails(testCacheMap)
        mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
        mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)
        mockCreateSubscriptionFailure(testNino, testCacheMap.getSummary(), isReleaseFourEnabled = false, isPropertyNextTaxYearEnabled = false)
        intercept[InternalServerException](await(result)).message must include("Successful response not received from submission")
        verifySubscriptionDetailsFetchAll(1)
        verifySubscriptionDetailsSave(MtditId, 0)
      }
    }

  }


  "The back url" when {
    s"point to the ${controllers.individual.business.routes.BusinessAccountingMethodController.show().url} when the income source is Business" in {
      TestCheckYourAnswersController.backUrl(incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)) mustBe
        controllers.individual.business.routes.BusinessAccountingMethodController.show().url
    }

    s"point to the ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url} when the income source is Both" in {
      TestCheckYourAnswersController.backUrl(incomeSource = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)) mustBe
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
    }

    s"point to the ${controllers.individual.business.routes.PropertyAccountingMethodController.show().url} when the income source is Property" in {
      TestCheckYourAnswersController.backUrl(incomeSource = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)) mustBe
        controllers.individual.business.routes.PropertyAccountingMethodController.show().url
    }
  }

  authorisationTests()

}
