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

import forms.agent.IncomeSourceForm._
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.common.IncomeSourceModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}


class IncomeSourceFormSpec extends PlaySpec with OneAppPerTest {

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
      val agentIncomeSourceMessage = "agent.error.income_source.invalid"

      "show an error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = incomeSourceForm.bind(emptyInput0)
        emptyTest0.hasErrors shouldBe true
      }

      "show an error when the both values are not checked" in {
        val bothFalseData = DataMap.IncomeSource("false", "false")
        val bothFalseActualResponse = incomeSourceForm.bind(bothFalseData)
        bothFalseActualResponse.hasErrors shouldBe true
        bothFalseActualResponse.errors.size shouldBe 1
        bothFalseActualResponse.errors.head.message shouldBe agentIncomeSourceMessage
      }
    }
    "The following submission should be valid" in {
      val testBothTrue = DataMap.individualIncomeSource("true", "true")
      incomeSourceForm isValidFor testBothTrue
      val testsOneTrue = DataMap.individualIncomeSource("false", "true")
      incomeSourceForm isValidFor testsOneTrue
    }

  }
}
