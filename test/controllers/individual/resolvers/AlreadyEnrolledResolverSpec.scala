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

package controllers.individual.resolvers

import config.featureswitch.FeatureSwitch.OptBackIn
import config.featureswitch.FeatureSwitching
import connectors.individual.subscription.mocks.MockSubscriptionConnector
import models.common.subscription.SubscriptionSuccess
import models.status.GetITSAStatus.{Annual, MTDMandated, MTDVoluntary}
import models.{CustomerLed, HmrcLedConfirmed, HmrcLedUnconfirmed, SessionData}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.MockGetITSAStatusService

import scala.concurrent.Future
import scala.language.postfixOps

class AlreadyEnrolledResolverSpec extends PlaySpec with MockSubscriptionConnector with MockGetITSAStatusService with FeatureSwitching {

  object TestAlreadyEnrolledResolver extends AlreadyEnrolledResolver(mockSubscriptionConnector, mockGetITSAStatusService, appConfig)

  val testNino: String = "test-nino"
  val testMTDITID: String = "test-mtditid"
  val testSessionData: SessionData = SessionData()

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(OptBackIn)
  }

  "resolve" must {
    "return the HMRC led unconfirmed route" when {
      "the user is already signed up with a channel of HMRC led unconfirmed" in {
        setupMockGetSubscription(testNino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDITID, Some(HmrcLedUnconfirmed))))))

        await(TestAlreadyEnrolledResolver.resolve(testNino, testSessionData)).url mustBe
          controllers.individual.handoffs.routes.CheckIncomeSourcesController.show.url
      }
    }
    "return the opt back in route" when {
      "the user has an annual status" in {
        enable(OptBackIn)
        setupMockGetSubscription(testNino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDITID, Some(HmrcLedConfirmed))))))
        mockGetITSAStatusSuccess(testNino)(Annual)

        await(TestAlreadyEnrolledResolver.resolve(testNino, testSessionData)).url mustBe
          controllers.individual.handoffs.routes.OptedOutController.show.url
      }
    }
    "return the already enrolled route" when {
      "the opt back in feature switch is disabled" in {
        setupMockGetSubscription(testNino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDITID, Some(HmrcLedConfirmed))))))

        await(TestAlreadyEnrolledResolver.resolve(testNino, testSessionData)).url mustBe
          controllers.individual.matching.routes.AlreadyEnrolledController.show.url
      }
      "the user is already signed up with a channel of confirmed triggered migrated with a non annual status" in {
        enable(OptBackIn)
        setupMockGetSubscription(testNino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDITID, Some(HmrcLedConfirmed))))))
        mockGetITSAStatusSuccess(testNino)(MTDMandated)

        await(TestAlreadyEnrolledResolver.resolve(testNino, testSessionData)).url mustBe
          controllers.individual.matching.routes.AlreadyEnrolledController.show.url
      }
      "the user is already signed up with a channel of customer sign up with a non annual status" in {
        enable(OptBackIn)
        setupMockGetSubscription(testNino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDITID, Some(CustomerLed))))))
        mockGetITSAStatusSuccess(testNino)(MTDVoluntary)

        await(TestAlreadyEnrolledResolver.resolve(testNino, testSessionData)).url mustBe
          controllers.individual.matching.routes.AlreadyEnrolledController.show.url
      }
    }
  }

}
