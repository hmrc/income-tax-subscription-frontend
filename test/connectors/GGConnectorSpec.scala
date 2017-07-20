/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import connectors.mocks.MockGGConnector
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HeaderCarrier

class GGConnectorSpec extends MockGGConnector {

  override implicit val hc = HeaderCarrier()
  val dummyResponse = Json.parse("{}")

//  def result = await(TestGovernmentGatewayEnrolConnector.enrol(governmentGatewayEnrolPayload))
//
//  "return OK response correctly" in {
//    mockGovernmentGatewayEnrol(governmentGatewayEnrolPayload)((OK, dummyResponse))
//    result.status shouldBe OK
//  }
//
//  "return BAD_REQUEST response correctly" in {
//    mockGovernmentGatewayEnrol(governmentGatewayEnrolPayload)((BAD_REQUEST, dummyResponse))
//    result.status shouldBe BAD_REQUEST
//  }
//
//  "return FORBIDDEN response correctly" in {
//    mockGovernmentGatewayEnrol(governmentGatewayEnrolPayload)((FORBIDDEN, dummyResponse))
//    result.status shouldBe FORBIDDEN
//
//  }
//  "return INTERNAL_SERVER_ERROR response correctly" in {
//    mockGovernmentGatewayEnrol(governmentGatewayEnrolPayload)((INTERNAL_SERVER_ERROR, dummyResponse))
//    result.status shouldBe INTERNAL_SERVER_ERROR
//
//  }
}
