/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.forms

import assets.MessageLookup
import core.forms.submapping.YesNoMapping
import core.forms.validation.ErrorMessageFactory
import core.forms.validation.testutils.{DataMap, _}
import core.models.{No, Yes}
import incometax.incomesource.models.RentUkPropertyModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class RentUkPropertyFormSpec extends PlaySpec with OneAppPerTest {

  import RentUkPropertyForm._

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

    "validate income type correctly" in {
      val emptyRentUkProperty = ErrorMessageFactory.error("error.rent-uk-property.empty")
      val invalidRentUkProperty = ErrorMessageFactory.error("error.rent-uk-property.invalid")
      val emptyOnlyIncomeSource = ErrorMessageFactory.error("error.rent-uk-property.only-source-empty")
      val invalidOnlyIncomeSource = ErrorMessageFactory.error("error.rent-uk-property.only-source-invalid")

      emptyRentUkProperty fieldErrorIs MessageLookup.Error.RentUkProperty.emptyRentUkProperty
      emptyRentUkProperty summaryErrorIs MessageLookup.Error.RentUkProperty.emptyRentUkProperty

      invalidRentUkProperty fieldErrorIs MessageLookup.Error.RentUkProperty.invalidRentUkProperty
      invalidRentUkProperty summaryErrorIs MessageLookup.Error.RentUkProperty.invalidRentUkProperty

      emptyOnlyIncomeSource fieldErrorIs MessageLookup.Error.RentUkProperty.emptyOnlyIncomeSource
      emptyOnlyIncomeSource summaryErrorIs MessageLookup.Error.RentUkProperty.emptyOnlyIncomeSource

      invalidOnlyIncomeSource fieldErrorIs MessageLookup.Error.RentUkProperty.invalidOnlyIncomeSource
      invalidOnlyIncomeSource summaryErrorIs MessageLookup.Error.RentUkProperty.invalidOnlyIncomeSource

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = rentUkPropertyForm.bind(emptyInput0)
      emptyTest0 assert rentUkProperty hasExpectedErrors emptyRentUkProperty

      val emptyInput = DataMap.rentUkProperty("")
      val emptyTest = rentUkPropertyForm.bind(emptyInput)
      emptyTest assert rentUkProperty hasExpectedErrors emptyRentUkProperty

      val invalidInput = DataMap.rentUkProperty("α")
      val invalidTest = rentUkPropertyForm.bind(invalidInput)
      invalidTest assert rentUkProperty hasExpectedErrors invalidRentUkProperty

      val emptyInputOnlyIncome0 = DataMap.rentUkProperty("Yes")
      val emptyTestOnlyIncome0 = rentUkPropertyForm.bind(emptyInputOnlyIncome0)
      emptyTestOnlyIncome0 assert onlySourceOfSelfEmployedIncome hasExpectedErrors emptyOnlyIncomeSource

      val emptyOnlyIncomeInput = DataMap.rentUkProperty("Yes", Some(""))
      val emptyOnlyIncomeTest = rentUkPropertyForm.bind(emptyOnlyIncomeInput)
      emptyOnlyIncomeTest assert onlySourceOfSelfEmployedIncome hasExpectedErrors emptyOnlyIncomeSource

      val invalidOnlyIncomeInput = DataMap.rentUkProperty("Yes", Some("α"))
      val invalidOnlyIncomeTest = rentUkPropertyForm.bind(invalidOnlyIncomeInput)
      invalidOnlyIncomeTest assert onlySourceOfSelfEmployedIncome hasExpectedErrors invalidOnlyIncomeSource
    }

    "The following submission should be valid" in {
      val testNo = DataMap.rentUkProperty(YesNoMapping.option_no)
      rentUkPropertyForm isValidFor testNo
      val testsYes = DataMap.rentUkProperty(YesNoMapping.option_yes, Some(YesNoMapping.option_yes))
      rentUkPropertyForm isValidFor testsYes
    }

  }
}
