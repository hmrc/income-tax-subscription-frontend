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

package controllers.agent


import config.featureswitch.FeatureSwitch.ReleaseFour
import config.featureswitch.FeatureSwitching
import models.common.{AccountingMethodModel, IncomeSourceModel}
import models.individual.business.SelfEmploymentData
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.Helpers._
import services.agent.mocks._
import services.mocks._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.InternalServerException
import utilities.SubscriptionDataKeys.MtditId
import utilities.SubscriptionDataUtil._
import utilities.agent.TestConstants.{testNino, _}
import utilities.agent.TestModels
import utilities.agent.TestModels.testCacheMap
import utilities.ImplicitDateFormatterImpl

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AgentControllerBaseSpec
  with MockSubscriptionDetailsService
  with MockClientRelationshipService
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

    def call(request: Request[AnyContent] = subscriptionRequest): Future[Result] = TestCheckYourAnswersController.show(request)

    "the user has not answered the income source question required" should {
      "redirect the user to the income sources page" in {
        mockFetchAllFromSubscriptionDetails(None)

        val result = call(subscriptionRequest)

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.agent.routes.IncomeSourceController.show().url)
      }
    }

    "There are both a matched nino and terms in Subscription Details " should {
      "return ok (200)" in {
        mockFetchIncomeSourceFromSubscriptionDetails(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false))
        mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)

        status(call()) must be(Status.OK)
      }

      "There are both a matched nino and terms in Subscription Details with release four enabled" should {
        "return ok (200)" in {
          enable(ReleaseFour)
          mockFetchIncomeSourceFromSubscriptionDetails(IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false))
          mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)
          mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
          mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)

          status(call()) must be(Status.OK)
          disable(ReleaseFour)
        }
      }
    }

    "There are no a matched nino in session" should {
      s"return redirect ${controllers.agent.matching.routes.ConfirmClientController.show().url}" in {
        mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)

        val result = call(subscriptionRequest.removeFromSession(ITSASessionKeys.NINO))

        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.agent.matching.routes.ConfirmClientController.show().url)
      }
    }
  }

  "Calling the submit action of the CheckYourAnswersController with an authorised user" when {
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
      "There is a matched nino and utr in Subscription Details  and the submission is successful" should {
        // generate a new nino specifically for this test,
        // since the default value in test constant may be used by accident
        lazy val newTestNino = new Generator().nextNino.nino
        lazy val authorisedAgentRequest = subscriptionRequest.addingToSession(
          ITSASessionKeys.ArnKey -> testARN,
          ITSASessionKeys.NINO -> newTestNino,
          ITSASessionKeys.UTR -> testUtr
        )

        lazy val result = call(authorisedAgentRequest)

        "return a redirect status (SEE_OTHER - 303) when release four is disabled" in {
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testSummary)

          mockCreateSubscriptionSuccess(testARN, newTestNino, testUtr, testSummary.getAgentSummary())

          status(result) must be(Status.SEE_OTHER)
          await(result)
          verifySubscriptionDetailsSave(MtditId, 1)
          verifySubscriptionDetailsFetchAll(2)
        }

        "return a redirect status (SEE_OTHER - 303) when release four is enabled" in {
          enable(ReleaseFour)
          setupMockSubscriptionDetailsSaveFunctions()
          mockFetchAllFromSubscriptionDetails(testSummary)
          mockGetSelfEmployments[Seq[SelfEmploymentData]]("Businesses")(None)
          mockGetSelfEmployments[AccountingMethodModel]("BusinessAccountingMethod")(None)

          mockCreateSubscriptionSuccess(testARN, newTestNino, testUtr, testSummary.getAgentSummary(), true)

          status(result) must be(Status.SEE_OTHER)
          await(result)
          disable(ReleaseFour)
        }

        s"redirect to '${controllers.agent.routes.ConfirmationController.show().url}'" in {
          redirectLocation(result) mustBe Some(controllers.agent.routes.ConfirmationController.show().url)
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
          mockCreateSubscriptionFailure(testARN, testNino, testUtr, TestModels.testCacheMap.getAgentSummary())

          val ex = intercept[InternalServerException](await(call(authorisedAgentRequest)))
          ex.message mustBe "Successful response not received from submission"
          verifySubscriptionDetailsSave(MtditId, 0)
          verifySubscriptionDetailsFetchAll(1)
        }
        "return a failure if create client relationship fails" ignore {
          val request = authorisedAgentRequest.addingToSession(ITSASessionKeys.ArnKey -> testARN)

          mockFetchAllFromSubscriptionDetails(TestModels.testCacheMap)
          mockCreateSubscriptionSuccess(testARN, testNino, testUtr, testCacheMap.getAgentSummary())

          val ex = intercept[InternalServerException](await(call(request)))
          ex.message mustBe "Failed to create client relationship"
          verifySubscriptionDetailsSave(MtditId, 0)
          verifySubscriptionDetailsFetchAll(1)
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
