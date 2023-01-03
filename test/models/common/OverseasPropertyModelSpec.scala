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

package models.common

import models.{Cash, DateModel}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsSuccess, Json}

class OverseasPropertyModelSpec extends AnyWordSpecLike with Matchers with OptionValues {
    "OverseasPropertyModel" should {
      "deserialize without confirmed field" in {
        val actual = Json.fromJson[OverseasPropertyModel](Json.parse("""{"startDate":{"day":"5","month":"11","year":"2021"}}"""))
        val expected = OverseasPropertyModel(startDate = Some(DateModel("5","11","2021")))
        actual mustBe JsSuccess(expected)
      }

      "deserialize with confirmed field" in {
        val actual = Json.fromJson[OverseasPropertyModel](
          Json.parse("""{"accountingMethod":"Cash","startDate":{"day":"5","month":"11","year":"2021"},"confirmed":true}""")
        )
        val expected = OverseasPropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("5","11","2021")), confirmed = true)
        actual mustBe JsSuccess(expected)
      }
    }
}
