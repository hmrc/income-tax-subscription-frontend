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

package connectors.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationTestConstants.testMtdId
import helpers.servicemocks.WireMockMethods
import models.common.subscription.SubscriptionRequest
import play.api.libs.json.{JsValue, Json}

object SubscriptionAPIStub extends WireMockMethods {

  private def uri(nino: String): String = s"/income-tax-subscription/subscription-v2/$nino"

  def stubGetSubscriptionResponse(nino: String)(responseCode: Int, response: JsValue = Json.obj("mtditId" -> testMtdId)): StubMapping = {
    when (
      method = GET,
      uri = uri(nino)
    ).thenReturn (
      status = responseCode,
      body = response
    )
  }

  def stubPostSubscription(request: SubscriptionRequest)(responseCode: Int, response: JsValue = Json.obj("mtditId" -> testMtdId)): StubMapping = {
    when (
      method = POST,
      uri = uri(request.nino),
      body = Json.toJson(request)
    ).thenReturn (
      status = responseCode,
      body = response
    )
  }

}
