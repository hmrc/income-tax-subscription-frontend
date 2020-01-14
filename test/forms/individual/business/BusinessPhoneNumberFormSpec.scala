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

package forms.individual.business

import assets.MessageLookup
import core.utils.TestConstants._
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import incometax.business.models.BusinessPhoneNumberModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages.Implicits._

class BusinessPhoneNumberFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.individual.business.BusinessPhoneNumberForm._

  "The BusinessPhoneNumberForm" should {
    "transform the data to the case class" in {
      val testInput = Map(phoneNumber -> testPhoneNumber)
      val expected = BusinessPhoneNumberModel(testPhoneNumber)
      val actual = businessPhoneNumberForm.bind(testInput).value
      actual shouldBe Some(expected)
    }

    "validate business name correctly" in {
      val maxLength = 24

      val empty = ErrorMessageFactory.error("error.business_phone_number.empty")
      val maxLen = ErrorMessageFactory.error("error.business_phone_number.maxLength")
      val invalid = ErrorMessageFactory.error("error.business_phone_number.invalid")

      empty fieldErrorIs MessageLookup.Error.BusinessPhoneNumber.empty
      empty summaryErrorIs MessageLookup.Error.BusinessPhoneNumber.empty

      maxLen fieldErrorIs MessageLookup.Error.BusinessPhoneNumber.maxLength
      maxLen summaryErrorIs MessageLookup.Error.BusinessPhoneNumber.maxLength

      invalid fieldErrorIs MessageLookup.Error.BusinessPhoneNumber.invalid
      invalid summaryErrorIs MessageLookup.Error.BusinessPhoneNumber.invalid


      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = businessPhoneNumberForm.bind(emptyInput0)
      emptyTest0 assert phoneNumber hasExpectedErrors empty

      val emptyInput = DataMap.busPhoneNumber("")
      val emptyTest = businessPhoneNumberForm.bind(emptyInput)
      emptyTest assert phoneNumber hasExpectedErrors empty

      val maxLengthInput = DataMap.busPhoneNumber("a" * maxLength + 1)
      val maxLengthTest = businessPhoneNumberForm.bind(maxLengthInput)
      maxLengthTest assert phoneNumber hasExpectedErrors maxLen

      val withinLimitInput = DataMap.busPhoneNumber("a" * maxLength)
      val withinLimitTest = businessPhoneNumberForm.bind(withinLimitInput)
      withinLimitTest assert phoneNumber doesNotHaveSpecifiedErrors maxLen

      val invalidInput = DataMap.busPhoneNumber("α")
      val invalidTest = businessPhoneNumberForm.bind(invalidInput)
      invalidTest assert phoneNumber hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val valid = DataMap.busPhoneNumber(testPhoneNumber)
      businessPhoneNumberForm.form isValidFor valid
    }
  }

}
