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

import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.individual.business.BusinessNameModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError

class BusinessNameFormSpec extends PlaySpec with GuiceOneAppPerSuite {

  import forms.individual.business.BusinessNameForm._

  "The businessNameForm" should {
    "transform the data to the case class" in {
      val testBusinessName = "Test business"
      val testInput = Map(businessName -> testBusinessName)
      val expected = BusinessNameModel(testBusinessName)
      val actual = businessNameForm.bind(testInput).value
      actual shouldBe Some(expected)
    }

    "validate business name correctly" should {
      val maxLength = 105

      val empty = "error.business_name.empty"
      val maxLen = "error.business_name.maxLength"
      val invalid = "error.business_name.invalid"

      "the map be empty" in {
        val emptyInput0 = DataMap.EmptyMap
        val emptyTest0 = businessNameForm.bind(emptyInput0)
        emptyTest0.errors must contain(FormError(businessName,empty))
      }

      "the name be empty" in {
        val emptyInput = DataMap.busName("")
        val emptyTest = businessNameForm.bind(emptyInput)
        emptyTest.errors must contain(FormError(businessName,empty))
      }

      "the name is too long" in {
        val maxLengthInput = DataMap.busName("a" * maxLength + 1)
        val maxLengthTest = businessNameForm.bind(maxLengthInput)
        maxLengthTest.errors must contain(FormError(businessName,maxLen))
      }

      "the name should be invalid" in {
        val invalidInput = DataMap.busName("α")
        val invalidTest = businessNameForm.bind(invalidInput)
        invalidTest.errors must contain(FormError(businessName,invalid))
      }

      "the name is max characters and acceptable" in {
        val withinLimitInput = DataMap.busName("a" * maxLength)
        val withinLimitTest = businessNameForm.bind(withinLimitInput)
        withinLimitTest.value mustNot contain(maxLen)
      }

      "The following submission should be valid" in {
        val valid = DataMap.busName("Test business")
        businessNameForm.form isValidFor valid
      }
    }

  }
}
