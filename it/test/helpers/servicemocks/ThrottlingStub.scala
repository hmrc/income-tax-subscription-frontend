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

package helpers.servicemocks

import helpers.WiremockHelper
import play.api.http.Status._
import services.ThrottleId

object ThrottlingStub extends WireMockMethods {

  private def throttleURI(throttleId: ThrottleId): String = {
    s"/income-tax-subscription/throttled?throttleId=$throttleId"
  }

  def stubThrottle(throttleId: ThrottleId)(throttled: Boolean): Unit = {
    val status: Int = if (throttled) SERVICE_UNAVAILABLE else OK
    when(
      method = POST,
      uri = throttleURI(throttleId).replace("?", "\\?") // uses regex behind the scenes, \ to escape ? char in regex
    ).thenReturn(status = status)
  }

  def verifyThrottle(throttleId: ThrottleId)(count: Int = 1): Unit = {
    WiremockHelper.verifyPost(uri = throttleURI(throttleId), count = Some(count))
  }

}
