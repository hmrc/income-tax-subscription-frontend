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

package forms.individual.incomesource

import assets.MessageLookup
import forms.individual.incomesource.RentUkPropertyForm._
import forms.submapping.YesNoMapping
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.individual.incomesource.RentUkPropertyModel
import models.{No, Yes}
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.data.FormError
import play.api.data.validation.Invalid
import play.api.i18n.Messages.Implicits._

class RentUkPropertyFormSpec extends PlaySpec with OneAppPerTest {

  "The RentUkProperty Form" should {
    "transform the request to the form case class when No is bound to rent uk property question" in {
      val testChoice = No
      val testInput = Map(rentUkProperty -> YesNoMapping.option_no)
      val expected = RentUkPropertyModel(testChoice, None)
      val actual = rentUkPropertyForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "transform the request to the form case class when Yes is bound to rent uk property question and Yes to only source of income" in {
      val testChoice = Yes
      val testInput = Map(rentUkProperty -> YesNoMapping.option_yes, onlySourceOfSelfEmployedIncome -> YesNoMapping.option_yes)
      val expected = RentUkPropertyModel(testChoice, Some(testChoice))
      val actual = rentUkPropertyForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "transform the request to the form case class when Yes is bound to rent uk property question and No to only source of income" in {
      val testInput = Map(rentUkProperty -> YesNoMapping.option_yes, onlySourceOfSelfEmployedIncome -> YesNoMapping.option_no)
      val expected = RentUkPropertyModel(Yes, Some(No))
      val actual = rentUkPropertyForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate income type correctly" when {
      val emptyRentUkProperty = "error.rent-uk-property.empty"
      val invalidRentUkProperty = "error.rent-uk-property.invalid"
      val emptyOnlyIncomeSource = "error.rent-uk-property.only-source-empty"
      val invalidOnlyIncomeSource = "error.rent-uk-property.only-source-invalid"

      "show an empty error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = rentUkPropertyForm.bind(emptyInput0)
        emptyTest0.errors must contain(FormError(rentUkProperty, emptyRentUkProperty))
      }

      "show an empty error when the input is empty" in {
        val emptyInput = DataMap.rentUkProperty("")
        val emptyTest = rentUkPropertyForm.bind(emptyInput)
        emptyTest.errors must contain(FormError(rentUkProperty, emptyRentUkProperty))
      }

      "show invalid when the input is invalid" in {
        val invalidInput = DataMap.rentUkProperty("α")
        val invalidTest = rentUkPropertyForm.bind(invalidInput)
        invalidTest.errors must contain(FormError(rentUkProperty, invalidRentUkProperty))
      }

      "show an OnlyIncomeSource-empty error when the map is empty" in {
        val emptyInputOnlyIncome0 = DataMap.rentUkProperty("Yes")
        val emptyTestOnlyIncome0 = rentUkPropertyForm.bind(emptyInputOnlyIncome0)
        emptyTestOnlyIncome0.errors must contain(FormError(onlySourceOfSelfEmployedIncome, emptyOnlyIncomeSource))
      }

      "show an OnlyIncomeSource-empty error when the input is empty" in {
        val emptyOnlyIncomeInput = DataMap.rentUkProperty("Yes", Some(""))
        val emptyOnlyIncomeTest = rentUkPropertyForm.bind(emptyOnlyIncomeInput)
        emptyOnlyIncomeTest.errors must contain(FormError(onlySourceOfSelfEmployedIncome, emptyOnlyIncomeSource))
      }

      "show OnlyIncomeSource-invalid when the input is invalid" in {
        val invalidOnlyIncomeInput = DataMap.rentUkProperty("Yes", Some("α"))
        val invalidOnlyIncomeTest = rentUkPropertyForm.bind(invalidOnlyIncomeInput)
        invalidOnlyIncomeTest.errors must contain(FormError(onlySourceOfSelfEmployedIncome, invalidOnlyIncomeSource))
      }
    }
    "The following submission should be valid" in {
      val testNo = DataMap.rentUkProperty(YesNoMapping.option_no)
      rentUkPropertyForm isValidFor testNo
      val testsYes = DataMap.rentUkProperty(YesNoMapping.option_yes, Some(YesNoMapping.option_yes))
      rentUkPropertyForm isValidFor testsYes
    }

  }
}
