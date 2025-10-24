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

package services

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitching
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataSuccessResponse
import models.SessionData
import org.mockito.Mockito.{never, times}
import play.api.libs.json.JsBoolean
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks.{MockSessionDataService, MockThrottlingConnector}

import scala.concurrent.Future

class ThrottlingServiceSpec extends MockThrottlingConnector with MockSessionDataService with FeatureSwitching {

  val throttlingService = new ThrottlingService(mockThrottlingConnector, mockSessionDataService, appConfig)
  val fuzzyResult: Result = mock[Result]

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
  }

  "throttle" should {
    "return a throttle success" when {
      "the feature switch is disabled" in {
        disable(ThrottlingFeature)

        val result: Future[Result] = throttlingService.throttled(mockThrottle)(fuzzyResult)

        await(result) mustBe fuzzyResult

        verifyGetThrottleStatusCalls(never)
      }
      "session indicates the user has already passed through the throttle" in {
        val sessionData = SessionData(Map(
          ITSASessionKeys.throttlePassed(mockThrottle) -> JsBoolean(true)
        ))

        val result: Future[Result] = throttlingService.throttled(mockThrottle, sessionData)(fuzzyResult)

        await(result) mustBe fuzzyResult

        verifyGetThrottleStatusCalls(never)
      }
      "the throttle call returned a successful response" in {
        notThrottled()
        mockSaveThrottlePassed(mockThrottle)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = throttlingService.throttled(mockThrottle)(fuzzyResult)

        await(result) mustBe fuzzyResult

        verifyGetThrottleStatusCalls(times(1))
      }
      "the throttle call failed, but the throttle was setup to open on failure" in {
        throttleFail()
        failOpen()
        mockSaveThrottlePassed(mockThrottle)(Right(SaveSessionDataSuccessResponse))

        val result: Future[Result] = throttlingService.throttled(mockThrottle)(fuzzyResult)

        await(result) mustBe fuzzyResult

        verifyGetThrottleStatusCalls(times(1))
      }
    }
    "redirect to the throttle call" when {
      "the throttle call returned false" in {
        throttled()

        val result: Future[Result] = throttlingService.throttled(mockThrottle)(fuzzyResult)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(failFuzzyUrl)

        verifyGetThrottleStatusCalls(times(1))
      }
      "the throttle call failed, but the throttle was setup to close on failure" in {
        throttleFail()
        failClosed()

        val result: Future[Result] = throttlingService.throttled(mockThrottle)(fuzzyResult)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(failFuzzyUrl)
      }
    }
  }
}
