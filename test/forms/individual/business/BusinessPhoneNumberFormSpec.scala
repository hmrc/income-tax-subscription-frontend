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

import core.utils.TestConstants._
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.individual.business.BusinessPhoneNumberModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError

class BusinessPhoneNumberFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.individual.business.BusinessPhoneNumberForm._

  "The BusinessPhoneNumberForm" should {
    "transform the data to the case class" in {
      val testInput = Map(phoneNumber -> testPhoneNumber)
      val expected = BusinessPhoneNumberModel(testPhoneNumber)
      val actual = businessPhoneNumberForm.bind(testInput).value
      actual shouldBe Some(expected)
    }

    "validate business name correctly" should {
      val maxLength = 24

      val empty = "error.business_phone_number.empty"
      val maxLen = "error.business_phone_number.maxLength"
      val invalid = "error.business_phone_number.invalid"


      "show an empty error when the map is empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = businessPhoneNumberForm.bind(emptyInput0)
        emptyTest0.errors must contain(FormError(phoneNumber, empty))
      }
      "show an empty error when the input is empty" in {
        val emptyInput = DataMap.busPhoneNumber("")
        val emptyTest = businessPhoneNumberForm.bind(emptyInput)
        emptyTest.errors must contain(FormError(phoneNumber, empty))
      }
      "show invalid when the input is invalid" in {
        val invalidInput = DataMap.busPhoneNumber("α")
        val invalidTest = businessPhoneNumberForm.bind(invalidInput)
        invalidTest.errors must contain(FormError(phoneNumber, invalid))
      }
      "show a maxlength error when the input is too long" in {
        val maxLengthInput = DataMap.busPhoneNumber("a" * maxLength + 1)
        val maxLengthTest = businessPhoneNumberForm.bind(maxLengthInput)
        maxLengthTest.errors must contain(FormError(phoneNumber, maxLen))
      }
      "show that the input is ok when it is on the upper boundary" in {
        val withinLimitInput = DataMap.busPhoneNumber("a" * maxLength)
        val withinLimitTest = businessPhoneNumberForm.bind(withinLimitInput)
        withinLimitTest.value mustNot contain(maxLen)
      }
    }

    "The following submission should be valid" in {
      val valid = DataMap.busPhoneNumber(testPhoneNumber)
      businessPhoneNumberForm.form isValidFor valid
    }
  }

}
