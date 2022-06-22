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

package services

import config.featureswitch.FeatureSwitch.ThrottlingFeature
import config.featureswitch.FeatureSwitching
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{never, verify}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import services.mocks.MockThrottlingConnector

class ThrottlingServiceSpec extends MockThrottlingConnector with FeatureSwitching {

  val throttlingService = new ThrottlingService(mockThrottlingConnector, appConfig)
  val fuzzyResult: Result = mock[Result]
  private val failOpenUrl = "failOpenUrl"
  private val failClosedUrl = "failClosedUrl"

  private def test(throttle: Throttle) = throttlingService.throttled(throttle) {
    fuzzyResult
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
  }

  "Throttling" when {
    "the throttling feature switch is disabled" should {
      "return the output Result from the throttled block without calling the throttle" in {
        disable(ThrottlingFeature)

        val eventualResult = test(TestOpenThrottle)
        await(eventualResult) mustBe fuzzyResult

        verify(mockThrottlingConnector, never).getThrottleStatus(ArgumentMatchers.any())(ArgumentMatchers.any())
      }
    }
    "the throttling feature switch is enabled" should {
      "return the output Result from the throttled block" when {
        "throttling succeeds in a fail open throttle" in {
          val eventualResult = test(TestOpenThrottle)
          await(eventualResult) mustBe fuzzyResult
        }
        "throttling succeeds in a fail closed throttle" in {
          val eventualResult = test(TestClosedThrottle)
          await(eventualResult) mustBe fuzzyResult
        }
        "throttling throws an exception in a fail open throttle" in {
          throttleFail()
          val eventualResult = test(TestOpenThrottle)
          await(eventualResult) mustBe fuzzyResult
        }
      }
      "redirect to the url provided by the specified throttle" when {
        "throttling fails in a fail open throttle" in {
          throttled()
          val eventualResult = test(TestOpenThrottle)
          status(eventualResult) mustBe SEE_OTHER
          redirectLocation(eventualResult) mustBe Some(failOpenUrl)
        }
        "throttling fails in a fail closed throttle" in {
          throttled()
          val eventualResult = test(TestClosedThrottle)
          status(eventualResult) mustBe SEE_OTHER
          redirectLocation(eventualResult) mustBe Some(failClosedUrl)
        }
        "throttling throws an exception in a fail closed throttle" in {
          throttleFail()
          val eventualResult = test(TestClosedThrottle)
          status(eventualResult) mustBe SEE_OTHER
          redirectLocation(eventualResult) mustBe Some(failClosedUrl)
        }
      }
    }
  }

  case object TestOpenThrottle extends Throttle {
    val throttleId = "TestOpenThrottle"
    val failOpen = true
    val callOnFail: Call = Call("test", failOpenUrl)
  }

  case object TestClosedThrottle extends Throttle {
    val throttleId = "TestClosedThrottle"
    val failOpen = false
    val callOnFail: Call = Call("test", failClosedUrl)
  }

}
