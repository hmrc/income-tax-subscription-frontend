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
import forms.validation.ErrorMessageFactory
import forms.validation.testutils.DataMap.DataMap
import forms.validation.testutils._
import models.individual.business.BusinessNameModel
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages.Implicits._

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

    "validate business name correctly" in {
      val maxLength = 105

      val empty = ErrorMessageFactory.error("error.business_name.empty")
      val maxLen = ErrorMessageFactory.error("error.business_name.maxLength")
      val invalid = ErrorMessageFactory.error("error.business_name.invalid")

      empty fieldErrorIs MessageLookup.Error.BusinessName.empty
      empty summaryErrorIs MessageLookup.Error.BusinessName.empty

      maxLen fieldErrorIs MessageLookup.Error.BusinessName.maxLength
      maxLen summaryErrorIs MessageLookup.Error.BusinessName.maxLength

      invalid fieldErrorIs MessageLookup.Error.BusinessName.invalid
      invalid summaryErrorIs MessageLookup.Error.BusinessName.invalid


      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = businessNameForm.bind(emptyInput0)
      emptyTest0 assert businessName hasExpectedErrors empty

      val emptyInput = DataMap.busName("")
      val emptyTest = businessNameForm.bind(emptyInput)
      emptyTest assert businessName hasExpectedErrors empty

      val maxLengthInput = DataMap.busName("a" * maxLength + 1)
      val maxLengthTest = businessNameForm.bind(maxLengthInput)
      maxLengthTest assert businessName hasExpectedErrors maxLen

      val withinLimitInput = DataMap.busName("a" * maxLength)
      val withinLimitTest = businessNameForm.bind(withinLimitInput)
      withinLimitTest assert businessName doesNotHaveSpecifiedErrors maxLen

      val invalidInput = DataMap.busName("α")
      val invalidTest = businessNameForm.bind(invalidInput)
      invalidTest assert businessName hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val valid = DataMap.busName("Test business")
      businessNameForm.form isValidFor valid
    }
  }

}
