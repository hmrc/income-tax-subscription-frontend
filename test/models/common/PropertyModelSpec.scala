/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{Cash, Current, DateModel}
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.libs.json.{JsSuccess, Json}
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class PropertyModelSpec extends WordSpecLike with Matchers with OptionValues {
  "PropertyModel" should {
    "deserialize without confirmed field" in {
      val actual = Json.fromJson[PropertyModel](Json.parse("""{"startDate":{"day":"5","month":"11","year":"2021"}}"""))
      val expected = PropertyModel(startDate = Some(DateModel("5","11","2021")))
      actual mustBe JsSuccess(expected)
    }

    "deserialize with confirmed field" in {
      val actual = Json.fromJson[PropertyModel](
        Json.parse("""{"accountingMethod":"Cash","startDate":{"day":"5","month":"11","year":"2021"},"confirmed":true}""")
      )
      val expected = PropertyModel(accountingMethod = Some(Cash), startDate = Some(DateModel("5","11","2021")), confirmed = true)
      actual mustBe JsSuccess(expected)
    }
  }
}
