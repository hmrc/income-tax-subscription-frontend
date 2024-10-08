/*
 * Copyright 2023 HM Revenue & Customs
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

package views.individual.eligibility

import forms.individual.business.CannotSignUpThisYearForm
import forms.individual.business.CannotSignUpThisYearForm.yesNo
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.individual.eligibility.CannotSignUpThisYear

class CannotSignUpThisYearViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[CannotSignUpThisYear]

  val testFormError: FormError = FormError(yesNo, "error.cannot-sign-up.invalid")

  class ViewTest(hasError: Boolean = false) {
    val page: HtmlFormat.Appendable = view(
      if (hasError) {
        CannotSignUpThisYearForm.cannotSignUpThisYearForm.withError(testFormError)
      } else {
        CannotSignUpThisYearForm.cannotSignUpThisYearForm
      },
      postAction = testCall)

    val document: Document = Jsoup.parse(page.body)

    val mainContent: Element = document.mainContent
    val year: Int = AccountingPeriodUtil.getCurrentTaxEndYear - 1
    val nextYear: Int = AccountingPeriodUtil.getCurrentTaxEndYear
  }

  "Cannot Sign Up View" should {
    "be using the correct template details" when {
      "There is no error" in new ViewTest {
        new TemplateViewTest(
          view = page,
          title = CannotSignUpMessages.heading,
          hasSignOutLink = true
        )
      }
      "There is an error" in new ViewTest(hasError = true) {
        new TemplateViewTest(
          view = page,
          title = CannotSignUpMessages.heading,
          hasSignOutLink = true, error = Some(testFormError)
        )
      }
    }
    "have a heading" in new ViewTest {
      document.mainContent.selectFirst("h1").text() mustBe CannotSignUpMessages.heading
    }

    "have paragraph 1" in new ViewTest {
      document.mainContent.selectFirst("p").text() mustBe CannotSignUpMessages.paragraph1(year, nextYear)
    }

    "have a the correct yes-no radio inputs" in new ViewTest {
      mainContent.mustHaveYesNoRadioInputs(selector = "fieldset")(
        name = CannotSignUpThisYearForm.yesNo,
        legend = CannotSignUpMessages.formHeading,
        isHeading = false,
        isLegendHidden = false,
        hint = None,
        errorMessage = None
      )
    }

    "have a Accept and Continue button" in new ViewTest {
      mainContent.getForm.getGovukSubmitButton.text mustBe CannotSignUpMessages.continueButton
    }

  }

  object CannotSignUpMessages {
    val heading = "You can sign up to Making Tax Digital for Income Tax from the next tax year"

    def paragraph1(year: Int, nextYear: Int) = s"There is something that has prevented you from signing up to the current tax year (6 April $year to 5 April $nextYear). For example, other income or activities, or the length of time you have been self-employed or a landlord."

    val formHeading = "Would you like to sign up for the next tax year?"
    val continueButton = "Continue"
  }
}
