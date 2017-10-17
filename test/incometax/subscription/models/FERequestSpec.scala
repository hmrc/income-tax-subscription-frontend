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

package incometax.subscription.models

import incometax.subscription.models.{Business, IncomeSourceType, SubscriptionRequest}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import core.utils.JsonUtils._
import core.utils.TestConstants

class FERequestSpec extends UnitSpec {

  "FERequest" should {
    "Provide the correct reader for FERequest" in {
      val feRequest = SubscriptionRequest(
        nino = TestConstants.testNino,
        incomeSource = Business
      )

      val request: JsValue = Json.toJson(feRequest)
      val expected = Json.fromJson[SubscriptionRequest](
        s"""{"nino" : "${TestConstants.testNino}",
           | "isAgent" : false,
           | "incomeSource":"${IncomeSourceType.business}"}""".stripMargin).get
      val actual = Json.fromJson[SubscriptionRequest](request).get
      actual shouldBe expected
    }
  }
}
