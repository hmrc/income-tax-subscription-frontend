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

import forms.validation.ErrorMessageFactory
import forms.validation.testutils.{DataMap, _}
import models.EmailModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class EmailFormSpec extends PlaySpec with OneAppPerTest {

  import EmailForm._

  "The emailForm " should {
    "transform the data to the case class" in {
      val testEmail = "ABC@gmsil.com"
      val testInput = Map(emailAddress -> testEmail)
      val expected = EmailModel(testEmail)
      val actual = emailForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate contact email correctly" in {
      val maxLength = emailMaxLength

      val empty = ErrorMessageFactory.error("error.contact_email.empty")
      val maxLen = ErrorMessageFactory.error("error.contact_email.maxLength")
      val invalid = ErrorMessageFactory.error("error.contact_email.invalid")

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = emailForm.bind(emptyInput0)
      emptyTest0 assert emailAddress hasExpectedErrors empty

      val emptyInput = DataMap.email("")
      val emptyTest = emailForm.bind(emptyInput)
      emptyTest assert emailAddress hasExpectedErrors empty

      val maxLengthInput = DataMap.email("a" * maxLength + 1)
      val maxLengthTest = emailForm.bind(maxLengthInput)
      maxLengthTest assert emailAddress hasExpectedErrors maxLen

      val withinLimitInput = DataMap.email("a" * maxLength)
      val withinLimitTest = emailForm.bind(withinLimitInput)
      withinLimitTest assert emailAddress doesNotHaveSpecifiedErrors maxLen

      val testInvalids = List[DataMap.DataMap](
        DataMap.email("a@a"),
        DataMap.email("a@a."),
        DataMap.email("@a.a"),
        DataMap.email("a.a@a")
      )
      testInvalids.foreach {
        invalidInput =>
          val invalidTest = emailForm.bind(invalidInput)
          invalidTest assert emailAddress hasExpectedErrors invalid
      }
    }

    "The following submissions should be valid" in {
      val valid = DataMap.email("test@example.com")
      emailForm isValidFor valid
      val valid2 = DataMap.email("test.test@example.com")
      emailForm isValidFor valid2
    }
  }

}