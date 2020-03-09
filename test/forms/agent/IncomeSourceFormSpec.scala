/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.agent

import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.individual.subscription.Business
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.data.FormError

class IncomeSourceFormSpec extends PlaySpec with OneAppPerTest {

  import forms.agent.IncomeSourceForm._

  "The IncomeSourceForm" should {
    "transform the request to the form case class" in {
      val testIncomeSource = option_business
      val testInput = Map(incomeSource -> testIncomeSource)
      val expected = Business
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" should {
      val empty = "agent.error.income_source.invalid"
      val invalid = "agent.error.income_source.invalid"

      "fail when nothing has been entered in the view" in {
        val res = incomeSourceForm.bind(Map.empty[String, String])
        res.errors should contain(FormError(incomeSource, empty))
      }

      "fail when it is not an expected value in the view" in {
        val res = incomeSourceForm.bind(Map(incomeSource -> "invalid"))
        res.errors should contain(FormError(incomeSource, invalid))
      }
    }

    "The Business submission should be valid" in {
      val testBusiness = DataMap.incomeSource(option_business)
      incomeSourceForm isValidFor testBusiness
    }
    "The Property submission should be valid" in {
      val testProperty = DataMap.incomeSource(option_property)
      incomeSourceForm isValidFor testProperty
    }
    "The Both business and property submission should be valid" in {
      val testBoth = DataMap.incomeSource(option_both)
      incomeSourceForm isValidFor testBoth
    }
  }

}
