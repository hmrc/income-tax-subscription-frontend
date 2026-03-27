/*
 * Copyright 2026 HM Revenue & Customs
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

package views.individual.tasklist.taxyear

import forms.individual.business.AccountingYearForm
import messagelookup.agent.MessageLookup
import models.{Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.WhenDoYouWantToStart
class WhenDoYouWantToStartViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  val whatYearToSignUp: WhenDoYouWantToStart = app.injector.instanceOf[WhenDoYouWantToStart]

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false),
        title = WhenDoYouWantToStart.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasError = true),
        title = WhenDoYouWantToStart.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().select("h1").text mustBe WhenDoYouWantToStart.heading
    }

    "have a body" which {
      "has a first paragraph" in {
        document().mainContent.selectNth("p", 1).text mustBe WhenDoYouWantToStart.paragraph
      }

      "has a second paragraph" in {
        document().mainContent.selectNth("p", 2).text mustBe WhenDoYouWantToStart.paragraph2
      }
    }

    "have a form" which {
      "has correct attributes" in {
        val form: Elements = document().select("form")
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" in {
        document().mustHaveRadioInput(selector = "fieldset")(
          name = AccountingYearForm.accountingYear,
          legend = WhenDoYouWantToStart.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(WhenDoYouWantToStart.currentYearOption),
              value = Some(Current.toString)
            ),
            RadioItem(
              content = Text(WhenDoYouWantToStart.nextYearOption),
              value = Some(Next.toString)
            )
          )
        )
      }

      "has a continue button" that {
        s"displays ${MessageLookup.Base.continue} when not in edit mode" in {
          document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }
  }

  private def page(editMode: Boolean, hasError: Boolean = false): Html =
    whatYearToSignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false, hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, hasError).body)

  object WhenDoYouWantToStart {
    val heading = "When do you want to start using Making Tax Digital for Income Tax?"
    val paragraph = "If you start in the current tax year, you’ll need to use software that works with Making Tax Digital for Income Tax to send any missed quarterly updates for the year so far."
    val paragraph2 = "Making Tax Digital for Income Tax is voluntary until 6 April 2026, so you will not get penalties for any missed quarterly updates before then."
    val currentYearOption: String = s"I want to start in the current tax year, ${taxYearEnd - 1} to $taxYearEnd"
    val nextYearOption: String = s"I want to start from the next tax year, $taxYearEnd to ${taxYearEnd + 1}"
  }
}
