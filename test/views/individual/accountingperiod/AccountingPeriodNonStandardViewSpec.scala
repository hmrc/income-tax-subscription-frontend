/*
 * Copyright 2025 HM Revenue & Customs
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

package views.individual.accountingperiod
import forms.individual.accountingperiod.AccountingPeriodNonStandardForm
import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.accountingPeriod.AccountingPeriodNonStandard
class AccountingPeriodNonStandardViewSpec extends ViewSpec {
  private val nonStandardAccountingPeriod: AccountingPeriodNonStandard = app.injector.instanceOf[AccountingPeriodNonStandard]
  private val testFormError: FormError = FormError(AccountingPeriodNonStandardForm.fieldName, "accounting-period-non-standard.form-error")
  "Non Standard Accounting Period" must {
    import NonStandardAccountingPeriodMessages._
    "have the correct template details" when {
      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = false,
        title = heading
      )
      "the page has an error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = false,
        title = heading,
        error = Some(testFormError)
      )
    }
    "have a heading" in {
      document().getH1Element.text mustBe heading
    }
    "have the correct first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe NonStandardAccountingPeriodMessages.paraOne
    }
    "contains a bullet list" which {
      def bulletList = document().mainContent.selectNth("ul", 1)
      "has a first item" in {
        bulletList.selectNth("li", 1).text mustBe NonStandardAccountingPeriodMessages.bulletOne
      }
      "has a second item" in {
        bulletList.selectNth("li", 2).text mustBe NonStandardAccountingPeriodMessages.bulletTwo
      }
    }
    "have a form" which {
      def form: Element = document().selectHead("form")
      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }
      "has the correct radio inputs" in {
        form.mustHaveYesNoRadioInputs(selector = "fieldset")(
          name = radioName,
          legend = NonStandardAccountingPeriodMessages.radioLegend,
          isHeading = false,
          isLegendHidden = false,
          hint = None,
          errorMessage = None,
        )
      }
      "has a continue button" in {
        form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
      }
    }
  }
  private def page(hasError: Boolean = false): Html = {
    nonStandardAccountingPeriod(
      if (hasError) AccountingPeriodNonStandardForm.nonStandardAccountingPeriodForm.withError(testFormError) else AccountingPeriodNonStandardForm.nonStandardAccountingPeriodForm,
      testCall,
      testBackUrl
    )
  }
  private def document(hasError: Boolean = false): Document =
    Jsoup.parse(page(hasError).body)
  private object NonStandardAccountingPeriodMessages {
    val heading: String = "Your business accounting period is not yet supported"
    val paraOne: String = "Making Tax Digital for Income Tax is only available to people who use these business accounting periods:"
    val bulletOne: String = "6 April to 5 April"
    val bulletTwo: String = "1 April to 31 March"
    val radioName: String = "yes-no"
    val radioLegend: String = "Would you like to sign up for next tax year?"
  }
}
