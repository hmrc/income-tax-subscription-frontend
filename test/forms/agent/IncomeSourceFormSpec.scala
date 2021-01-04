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

package forms.agent

import forms.agent.IncomeSourceForm._
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.common.IncomeSourceModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec


class IncomeSourceFormSpec extends PlaySpec {

  "The IncomeSource Form" should {
    "transform the request to the form case class when both checked values are bound to income source" in {

      val testInput = Map(business -> "true", ukProperty -> "true")
      val expected = IncomeSourceModel(true, true, false)
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "transform the request to the form case class when only self-employed is checked" in {
      val testInput = Map(business -> "true", ukProperty -> "false", foreignProperty -> "false")
      val expected = IncomeSourceModel(true, false, false)
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "transform the request to the form case class when when only uk property is checked" in {
      val testInput = Map(business -> "false", ukProperty -> "true", foreignProperty -> "false")
      val expected = IncomeSourceModel(false, true, false)
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "transform the request to the form case class when when only foreign property is checked" in {
      val testInput = Map(business -> "false", ukProperty -> "false", foreignProperty -> "true")
      val expected = IncomeSourceModel(false, false, true)
      val actual = incomeSourceForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income source with incorrect data" when {

      val invalidAgentIncomeSourceMessage = "agent.error.income_source.invalid"

      "show an error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = incomeSourceForm.bind(emptyInput0)
        emptyTest0.hasErrors shouldBe true
      }

      "show an error when the all values are not checked" in {
        val allFalseData = DataMap.incomeSource("false", "false", "false")
        val allFalseActualResponse = incomeSourceForm.bind(allFalseData)
        allFalseActualResponse.hasErrors shouldBe true
        allFalseActualResponse.errors.size shouldBe 1
        allFalseActualResponse.errors.head.message shouldBe invalidAgentIncomeSourceMessage

      }
      "The following submission should be valid" in {
        val testAllTrue = DataMap.incomeSource("true", "true", "true")
        incomeSourceForm isValidFor testAllTrue
        val testsPropFPropTrue = DataMap.incomeSource("false", "true", "true")
        incomeSourceForm isValidFor testsPropFPropTrue
        val testsBusFPropTrue = DataMap.incomeSource("true", "false", "true")
        incomeSourceForm isValidFor testsBusFPropTrue
        val testsBusPropTrue = DataMap.incomeSource("true", "true", "false")
        incomeSourceForm isValidFor testsBusPropTrue
        val testsBusTrue = DataMap.incomeSource("true", "false", "false")
        incomeSourceForm isValidFor testsBusTrue
        val testsPropTrue = DataMap.incomeSource("false", "true", "false")
        incomeSourceForm isValidFor testsPropTrue
        val testsFPropTrue = DataMap.incomeSource("false", "false", "true")
        incomeSourceForm isValidFor testsFPropTrue
      }

    }
  }
}
