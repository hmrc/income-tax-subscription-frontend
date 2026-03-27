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

package views.agent.tasklist.taxyear

import forms.agent.AccountingYearForm
import models.{Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, RadioItem, Text}
import utilities.ViewSpec
import views.html.agent.tasklist.taxyear.WhenDoYouWantToStart

class WhenDoYouWantToStartViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear

  private val whatYearToSignUp: WhenDoYouWantToStart = app.injector.instanceOf[WhenDoYouWantToStart]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "agent.error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, clientName = fullName, clientNino = nino),
        isAgent = true,
        title = WhenDoYouWantToStart.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasError = true),
        isAgent = true,
        title = WhenDoYouWantToStart.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = WhenDoYouWantToStart.heading,
        caption = WhenDoYouWantToStart.agentCaption,
        isSection = false
      )
    }

    "have a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe WhenDoYouWantToStart.paragraph1
    }

    "have a second paragraph" which {
      def para: Element = document().mainContent.selectNth("p", 2)

      "has the correct text" in {
        para.text mustBe WhenDoYouWantToStart.paragraph2
      }
      "has a link within the text" which {
        def link: Element = para.selectHead("a")

        "has the correct text" in {
          link.text mustBe WhenDoYouWantToStart.paragraph2LinkText
        }
        "has the correct attributes" in {
          link.attr("href") mustBe "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"
          link.attr("target") mustBe "_blank"
          link.attr("rel") mustBe Seq(
            "noopener",
            "noreferrer"
          ).mkString(" ")
        }
      }
    }

    "have a third paragraph and have the correct inset" in {
      document().mainContent.selectNth(".govuk-inset-text", 1).text mustBe WhenDoYouWantToStart.paragraph3
    }

    "have a second heading" in {
      document().mainContent.selectNth("h2", 2).text mustBe WhenDoYouWantToStart.heading2
    }

    "have a fourth paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe WhenDoYouWantToStart.paragraph4
    }

    "have a form" which {
      def form: Element = document().selectHead("form")

      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" in {
        form.mustHaveRadioInput(selector = "fieldset")(
          name = AccountingYearForm.accountingYear,
          legend = WhenDoYouWantToStart.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(WhenDoYouWantToStart.currentYearOption),
              value = Some(Current.toString),
              hint = Some(Hint(content = Text(WhenDoYouWantToStart.currentYearOptionHint))),
            ),
            RadioItem(
              content = Text(WhenDoYouWantToStart.nextYearOption),
              value = Some(Next.toString),
            )
          )
        )
      }

      "has a submit button" that {
        "displays Continue" in {
          val button: Element = form.getSubmitButton
          button.text mustBe WhenDoYouWantToStart.continue
        }
      }
    }

    "have the fifth paragraph" in {
      val paragraph: Elements = document().mainContent.select(".govuk-form-group").select(".govuk-body")
      paragraph.text must include(WhenDoYouWantToStart.signUpAnotherClient)
      paragraph.select("a.govuk-link").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private def page(editMode: Boolean, clientName: String = fullName, clientNino: String = nino, hasError: Boolean = false): Html =
    whatYearToSignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      clientName,
      clientNino,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false,
                       clientName: String = fullName,
                       clientNino: String = nino,
                       hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, clientName, clientNino, hasError).body)

  private object WhenDoYouWantToStart {
    val heading = "You can sign up this client"
    val agentCaption: String = fullName + " - " + nino
    val paragraph1 = s"You can sign up your client during the current tax year (from 6 April ${taxYearEnd - 1}) or from next tax year (from 6 April $taxYearEnd)."
    val paragraph2LinkText = "sending quarterly updates (opens in new tab)"
    val paragraph2 = s"Find out more about $paragraph2LinkText."
    val paragraph3 = s"Your client must still submit their Self Assessment tax returns for ${taxYearEnd - 1} to $taxYearEnd as normal."
    val heading2 = "When do you want to start using Making Tax Digital for Income Tax?"
    val paragraph4 = "I want to sign up my client now, and:"
    val currentYearOption: String = s"start in the current tax year, ${taxYearEnd - 1} to $taxYearEnd"
    val currentYearOptionHint = "You or your client will need to use your software to send any missed quarterly updates for the year so far"
    val nextYearOption: String = s"start from the next tax year, $taxYearEnd to ${taxYearEnd + 1}"
    val continue: String = "Continue"
    val signUpAnotherClient = "Or you can check if you can sign up another client. We will not save the details you entered about FirstName LastName."
  }
}