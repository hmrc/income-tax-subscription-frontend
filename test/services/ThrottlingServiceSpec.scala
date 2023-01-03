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

import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitching
import org.mockito.Mockito.{never, times}
import play.api.mvc.Result
import play.api.test.Helpers._
import services.mocks.MockThrottlingConnector

class ThrottlingServiceSpec extends MockThrottlingConnector with FeatureSwitching {

  val throttlingService = new ThrottlingService(mockThrottlingConnector, appConfig)
  val fuzzyResult: Result = mock[Result]

  private def test(throttle: Throttle) = throttlingService.throttled(throttle) {
    fuzzyResult
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
  }

  "Throttle result" when {
    "the throttling feature switch is disabled" should {
      "return None" in {
        disable(ThrottlingFeature)
        throttlingService.throttleResult(mockThrottle) mustBe None
        verifyGetThrottleStatusCalls(never())
      }
    }
    "the throttling feature switch is enabled" when {
      "the throttle is not throttled" should {
        "return Some(Future(Success(true)))" in {
          notThrottled()
          val maybeEventualBoolean = throttlingService.throttleResult(mockThrottle)
          maybeEventualBoolean.isDefined mustBe true
          maybeEventualBoolean.get.isCompleted mustBe true
          maybeEventualBoolean.get.map { a => a mustBe true }
          verifyGetThrottleStatusCalls(times(1))
        }
      }
      "the throttle is throttled" should {
        "return Some(Future(Success(false)))" in {
          throttled()
          val maybeEventualBoolean = throttlingService.throttleResult(mockThrottle)
          maybeEventualBoolean.isDefined mustBe true
          maybeEventualBoolean.get.isCompleted mustBe true
          maybeEventualBoolean.get.map { a => a mustBe false }
          verifyGetThrottleStatusCalls(times(1))
        }
      }
    }
  }

  "Throttling" when {
    "the throttling feature switch is disabled" should {
      "return the output Result from the throttled block without calling the throttle" in {
        disable(ThrottlingFeature)
        await(test(mockThrottle)) mustBe fuzzyResult
        verifyGetThrottleStatusCalls(never)
      }
    }

    "the throttling feature switch is enabled" when {
      "using a fail open throttle" when {
        "throttling succeeds" should {
          "return the output Result from the throttled block" in {
            failOpen()
            notThrottled()
            await(test(mockThrottle)) mustBe fuzzyResult
            verifyGetThrottleStatusCalls(times(1))
          }
        }
        "throttling fails" should {
          "redirect to the url provided by the specified throttle" in {
            failOpen()
            throttled()
            val eventualResult = test(mockThrottle)
            status(eventualResult) mustBe SEE_OTHER
            redirectLocation(eventualResult) mustBe Some(failFuzzyUrl)
            verifyGetThrottleStatusCalls(times(1))
          }
        }
        "throttling throws an exception" should {
          "redirect to the url provided by the specified throttle" in {
            failOpen()
            throttleFail()
            await(test(mockThrottle)) mustBe fuzzyResult
            verifyGetThrottleStatusCalls(times(1))
          }
        }
      }
      "using a fail closed throttle" when {
        "throttling succeeds" should {
          "return the output Result from the throttled block" in {
            failClosed()
            notThrottled()
            val eventualResult = test(mockThrottle)
            await(eventualResult) mustBe fuzzyResult
            verifyGetThrottleStatusCalls(times(1))
          }
        }
        "throttling fails" should {
          "redirect to the url provided by the specified throttle" in {
            failClosed()
            throttled()
            val eventualResult = test(mockThrottle)
            status(eventualResult) mustBe SEE_OTHER
            redirectLocation(eventualResult) mustBe Some(failFuzzyUrl)
            verifyGetThrottleStatusCalls(times(1))
          }
        }
        "throttling throws an exception" should {
          "redirect to the url provided by the specified throttle" in {
            failClosed()
            throttleFail()
            val eventualResult = test(mockThrottle)
            status(eventualResult) mustBe SEE_OTHER
            redirectLocation(eventualResult) mustBe Some(failFuzzyUrl)
            verifyGetThrottleStatusCalls(times(1))
          }
        }
      }
    }
  }
}
