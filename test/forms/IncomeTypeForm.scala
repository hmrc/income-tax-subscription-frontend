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

package forms

import models.IncomeTypeModel
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatest.Matchers._

class IncomeTypeFormSpec extends PlaySpec with OneAppPerTest {

  import IncomeTypeForm._

  "The IncomeTypeForm" should {
    "transform the request to the form case class" in {
      val testIncomeType = "ABC"
      val testInput = Map(incomeType -> testIncomeType)
      val expected = IncomeTypeModel(testIncomeType)
      val actual = incomeTypeForm.bind(testInput).value

      actual shouldBe Some(expected)
    }
  }

}
