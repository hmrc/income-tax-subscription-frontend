/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitching
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import models.common.{IncomeSourceModel, OverseasPropertyModel, PropertyModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.agent.mocks._
import services.mocks._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.InternalServerException
import utilities.ImplicitDateFormatterImpl
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey, MtditId}
import utilities.SubscriptionDataUtil._
import utilities.agent.TestConstants._
import utilities.agent.TestModels
import utilities.agent.TestModels.testAccountingMethodProperty
import views.html.agent.CheckYourAnswers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockClientRelationshipService
  with MockSubscriptionOrchestrationService
  with MockIncomeTaxSubscriptionConnector
  with MockAuditingService
  with FeatureSwitching {
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)
  val mockCheckYourAnswers: CheckYourAnswers = mock[CheckYourAnswers]

  override def beforeEach(): Unit = {
    reset(mockCheckYourAnswers)
    super.beforeEach()
  }

  override val controllerName: String = "CheckYourAnswersController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "show" -> TestCheckYourAnswersController.show,
    "submit" -> TestCheckYourAnswersController.submit
  )

  object TestCheckYourAnswersController extends CheckYourAnswersController(
    mockAuditingService,
    mockAuthService,
    MockSubscriptionDetailsService,
    mockSubscriptionOrchestrationService,
    mockIncomeTaxSubscriptionConnector,
    mockImplicitDateFormatter,
    mockCheckYourAnswers
  )

  val testPropertyModel: PropertyModel = PropertyModel(
    accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
    startDate = startDate
  )

  val testOverseasPropertyModel: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = testAccountingMethodProperty.propertyAccountingMethod,
    startDate = startDate
  )

  "Show with an authorised user" when {

    def call(request: Request[AnyContent] = subscriptionRequest): Future[Result] = TestCheckYourAnswersController.show(request)

    "the user has not answered the income source question required" should {
      "redirect the user to the income sources page" in {
        mockFetchAllFromSubscriptionDetails(None)

        val result = call(subscriptionRequest)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)
      }
    }

    "there are both a matched nino and terms in Subscription Details " should {
      "return ok (200)" in {
        mockFetchIncomeSourceFromSubscriptionDetails(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false))
        mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)
        mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
        mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)
        mockFetchProperty(None)
        mockFetchOverseasProperty(None)

        when(mockCheckYourAnswers(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(HtmlFormat.empty)

        status(call()) must be(Status.OK)
      }
    }

    "There are no a matched nino in session" should {
      "redirect the user to the confirm client page" in {
        mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)

        val result = call(subscriptionRequest.removeFromSession(ITSASessionKeys.NINO))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }
  }

  "Submit with an authorised user" when {
    lazy val testSummary = TestModels.testCacheMap

    def call(request: Request[AnyContent]): Future[Result] = TestCheckYourAnswersController.submit(request)

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        val request = subscriptionRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN).removeFromSession(ITSASessionKeys.NINO)

        mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)
        val result = call(request)

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }

    "The agent is authorised and" should {
      "There is a matched nino and utr in Subscription Details and the submission is successful" should {
        // generate a new nino specifically for this test,
        // since the default value in test constant may be used by accident
        lazy val newTestNino = new Generator().nextNino.nino
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(
          ITSASessionKeys.ArnKey -> testARN,
          ITSASessionKeys.NINO -> newTestNino,
          ITSASessionKeys.UTR -> testUtr
        )

        lazy val result = call(authorisedAgentRequest)

        "return a redirect status (SEE_OTHER - 303)" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testSummary)
          mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(testPropertyModel)
          mockFetchOverseasProperty(testOverseasPropertyModel)

          mockCreateSubscriptionSuccess(
            testARN,
            newTestNino,
            testUtr,
            testSummary.getAgentSummary(property = testPropertyModel, overseasProperty = testOverseasPropertyModel, isReleaseFourEnabled = true),
            isReleaseFourEnabled = true
          )

          status(result) must be(Status.SEE_OTHER)
          await(result)
        }

        "redirect the user to the agent confirmation agent page" in {
          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationAgentController.show().url)
        }
      }

      "When the submission is unsuccessful" should {
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(
          ITSASessionKeys.ArnKey -> testARN,
          ITSASessionKeys.NINO -> testNino,
          ITSASessionKeys.UTR -> testUtr
        )

        "return a failure if subscription fails" in {
          mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)
          mockGetSelfEmployments[Seq[SelfEmploymentData]](BusinessesKey)(None)
          mockGetSelfEmployments[AccountingMethodModel](BusinessAccountingMethod)(None)
          mockFetchProperty(testPropertyModel)
          mockFetchOverseasProperty(testOverseasPropertyModel)
          mockCreateSubscriptionFailure(
            testARN,
            testNino,
            testUtr,
            TestModels.testCacheMap.getAgentSummary(property = testPropertyModel, overseasProperty = testOverseasPropertyModel, isReleaseFourEnabled = true),
            isReleaseFourEnabled = true
          )

          val ex = intercept[InternalServerException](await(call(authorisedAgentRequest)))
          ex.message mustBe "Successful response not received from submission"
          verifySubscriptionDetailsSave(MtditId, 0)
        }
      }
    }
  }

  "The back url" should {
    s"point to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url}" when {
      "on the property only journey" in {
        TestCheckYourAnswersController.backUrl(
          IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
        ) mustBe business.routes.PropertyAccountingMethodController.show().url
      }
      "on the property and business journey" in {
        TestCheckYourAnswersController.backUrl(
          IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
        ) mustBe business.routes.PropertyAccountingMethodController.show().url
      }
    }

    s"point to ${controllers.agent.business.routes.BusinessAccountingMethodController.show().url}" when {
      "on the business only journey" in {
        TestCheckYourAnswersController.backUrl(
          IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
        ) mustBe controllers.agent.business.routes.BusinessAccountingMethodController.show().url
      }
    }

    s"point to ${controllers.agent.business.routes.PropertyAccountingMethodController.show().url} on the property journey" in {
      TestCheckYourAnswersController.backUrl(
        IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)
      ) mustBe controllers.agent.business.routes.PropertyAccountingMethodController.show().url
    }
  }

  authorisationTests()
}
