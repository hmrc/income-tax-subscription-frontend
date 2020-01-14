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
import core.models.Yes
import forms.individual.business.MatchTaxYearForm._
import forms.submapping.YesNoMapping
import forms.validation.ErrorMessageFactory
import forms.validation.testutils._
import forms.validation.testutils.DataMap.DataMap
import incometax.business.models.MatchTaxYearModel
import org.scalatest.Matchers._
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._

class MatchTaxYearFormSpec extends PlaySpec with OneAppPerTest {

  "The MatchTaxYearForm" should {
    "transform the request to the form case class" in {
      val testMatchTaxYear = Yes
      val testInput = Map(matchTaxYear -> YesNoMapping.option_yes)
      val expected = MatchTaxYearModel(testMatchTaxYear)
      val actual = matchTaxYearForm.bind(testInput).value

      actual shouldBe Some(expected)
    }

    "validate do you match the tax year answered correctly" in {
      val empty = ErrorMessageFactory.error("error.business.match_tax_year.empty")
      val invalid = ErrorMessageFactory.error("error.business.match_tax_year.invalid")

      empty fieldErrorIs MessageLookup.Error.Business.MatchTaxYear.empty
      empty summaryErrorIs MessageLookup.Error.Business.MatchTaxYear.empty

      invalid fieldErrorIs MessageLookup.Error.Business.MatchTaxYear.invalid
      invalid summaryErrorIs MessageLookup.Error.Business.MatchTaxYear.invalid

      val emptyInput0 = DataMap.EmptyMap
      val emptyTest0 = matchTaxYearForm.bind(emptyInput0)
      emptyTest0 assert matchTaxYear hasExpectedErrors empty

      val emptyInput = DataMap.matchTaxYear("")
      val emptyTest = matchTaxYearForm.bind(emptyInput)
      emptyTest assert matchTaxYear hasExpectedErrors empty

      val invalidInput = DataMap.matchTaxYear("α")
      val invalidTest = matchTaxYearForm.bind(invalidInput)
      invalidTest assert matchTaxYear hasExpectedErrors invalid
    }

    "The following submission should be valid" in {
      val testYes = DataMap.matchTaxYear(YesNoMapping.option_yes)
      matchTaxYearForm isValidFor testYes
      val testNo = DataMap.matchTaxYear(YesNoMapping.option_no)
      matchTaxYearForm isValidFor testNo
    }
  }

}
