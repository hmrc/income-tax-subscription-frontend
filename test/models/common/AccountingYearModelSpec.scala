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

import models.Current
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class AccountingYearModelSpec extends UnitSpec {

  "AccountingYearModel" should {

    "deserialize without confirmed field" in {
      val actual = Json.fromJson[AccountingYearModel](Json.parse("""{"accountingYear":"CurrentYear"}"""))
      val expected = AccountingYearModel(Current)
      actual mustBe JsSuccess(expected)
    }

    "deserialize with confirmed field" in {
      val actual = Json.fromJson[AccountingYearModel](Json.parse("""{"accountingYear":"CurrentYear","confirmed":true}"""))
      val expected = AccountingYearModel(Current, true)
      actual mustBe JsSuccess(expected)
    }
  }
}