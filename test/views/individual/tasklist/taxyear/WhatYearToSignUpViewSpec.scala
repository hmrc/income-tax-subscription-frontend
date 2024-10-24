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
import views.html.individual.tasklist.taxyear.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = false),
        title = WhatYearToSignUp.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasBackLink = true, hasError = true),
        title = WhatYearToSignUp.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().select("h1").text mustBe WhatYearToSignUp.heading
    }

    "have a body" which {
      "has a first paragraph" in {
        document().mainContent.select("p").get(0).text mustBe WhatYearToSignUp.paragraph
      }

      "has a second paragraph with a link" in {
        document().mainContent.select("p").get(1).text mustBe WhatYearToSignUp.paragraphTwo
        val link = document().mainContent.select(".govuk-link").get(0)
        link.text mustBe WhatYearToSignUp.LinkText
        link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"
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
          legend = WhatYearToSignUp.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(WhatYearToSignUp.currentYearOption),
              value = Some(Current.toString),
              hint = Some(Hint(content = Text(WhatYearToSignUp.currentYearOptionHint)))
            ),
            RadioItem(
              content = Text(WhatYearToSignUp.nextYearOption),
              value = Some(Next.toString)
            )
          )
        )
      }

      "has a continue button" that {
        s"displays ${MessageLookup.Base.continue} when not in edit mode" in {
          document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }

        s"displays ${MessageLookup.Base.update} when in edit mode" in {
          document(editMode = true).select("button[id=continue-button]").text mustBe MessageLookup.Base.update
        }
      }
    }
  }

  private def page(editMode: Boolean, hasBackLink: Boolean, hasError: Boolean = false): Html =
    whatYearToSignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      backUrl = if (hasBackLink) Some(testBackUrl) else None,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false, hasBackLink: Boolean = false, hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, hasBackLink = hasBackLink, hasError).body)

  object WhatYearToSignUp {
    val heading = "When do you want to start using Making Tax Digital for Income Tax?"
    val paragraph = s"You can sign up during the current tax year (from 6 April ${taxYearPrevious}) or from next tax year (from 6 April ${taxYearEnd})."
    val paragraphTwo = "Find out more about how and when to send quarterly updates (opens in new tab)."
    val LinkText = "how and when to send quarterly updates (opens in new tab)"
    val currentYearOptionHint = "You’ll need to use your software to send any missed quarterly updates for the year so far"
    val currentYearOption = "Current tax year"
    val nextYearOption = "Next tax year"

    def filingDateOne: String = s"6 April to 5 July"

    def deadlineDateOne: String = s"5 August"

    def filingDateTwo: String = s"6 July to 5 October"

    def deadlineDateTwo: String = s"5 November"

    def filingDateThree: String = s"6 October to 5 January"

    def deadlineDateThree: String = s"5 February"

    def filingDateFour: String = s"6 January to 5 April"

    def deadlineDateFour: String = s"5 May"
  }
}
