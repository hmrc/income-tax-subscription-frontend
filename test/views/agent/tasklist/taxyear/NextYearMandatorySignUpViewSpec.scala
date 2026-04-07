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

import config.featureswitch.FeatureSwitch.TaxYear26To27Plus
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
import views.html.agent.tasklist.taxyear.NextYearMandatorySignUp

class NextYearMandatorySignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYear26To27Plus)
  }

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear

  private val nextYearMandatorySignUp: NextYearMandatorySignUp = app.injector.instanceOf[NextYearMandatorySignUp]
  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "agent.error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false, clientName = fullName, clientNino = nino),
        isAgent = true,
        title = NextYearMandatorySignUp.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasError = true),
        isAgent = true,
        title = NextYearMandatorySignUp.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = NextYearMandatorySignUp.heading,
        caption = NextYearMandatorySignUp.agentCaption,
        isSection = false
      )
    }

    "have a first paragraph" which {
      "leads into the bullet list" when {
        "the 26-27 feature switch is disabled" in {
          document().mainContent.selectNth("p", 1).text mustBe NextYearMandatorySignUp.paragraph1Pre2627
        }
      }
      "is an independent paragraph" when {
        "the 26-27 feature switch is enabled" in {
          enable(TaxYear26To27Plus)

          document().mainContent.selectNth("p", 1).text mustBe NextYearMandatorySignUp.paragraph1Post2627
        }
      }
    }

    "don't have a bullet list" when {
      "the 26-27 feature switch is enabled" in {
        enable(TaxYear26To27Plus)

        document().mainContent.selectOptionalNth("ul", 1) mustBe None
      }
    }

    "have a bullet list" in {
      def bulletList: Element = document().mainContent.selectNth("ul", 1)

      bulletList.selectNth("li", 1).text mustBe NextYearMandatorySignUp.bullet1
      bulletList.selectNth("li", 2).text mustBe NextYearMandatorySignUp.bullet2
      bulletList.selectNth("li", 3).text mustBe NextYearMandatorySignUp.bullet3
    }

    "have a third paragraph and have the correct inset" in {
      document().mainContent.selectNth(".govuk-inset-text", 1).text mustBe NextYearMandatorySignUp.insert
    }

    "have a second heading" in {
      document().mainContent.selectNth("h2", 2).text mustBe NextYearMandatorySignUp.subheading
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
          legend = NextYearMandatorySignUp.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(NextYearMandatorySignUp.currentYearOption),
              value = Some(Current.toString),
              hint = Some(Hint(content = Text(NextYearMandatorySignUp.currentYearOptionHint))),
            ),
            RadioItem(
              content = Text(NextYearMandatorySignUp.nextYearOption),
              value = Some(Next.toString),
            )
          )
        )
      }

      "has a submit button" that {
        "displays Continue" in {
          val button: Element = form.getSubmitButton
          button.text mustBe NextYearMandatorySignUp.continue
        }
      }
    }

    "have the fifth paragraph" in {
      val paragraph: Elements = document().mainContent.select(".govuk-form-group").select(".govuk-body")
      paragraph.text must include(NextYearMandatorySignUp.signUpAnotherClient)
      paragraph.select("a.govuk-link").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private def page(editMode: Boolean, clientName: String = fullName, clientNino: String = nino, hasError: Boolean = false): Html =
    nextYearMandatorySignUp(
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

  private object NextYearMandatorySignUp {
    val heading: String = s"Your client must use Making Tax Digital for Income Tax next tax year, $taxYearEnd to ${taxYearEnd + 1}"
    val agentCaption: String = fullName + " - " + nino
    val paragraph1Pre2627 = "They can choose to sign up early, so you’re both prepared to use the service. This will mean that you will:"
    val paragraph1Post2627 = "They can choose to sign up early, so you’re both prepared to use the service."
    val bullet1 = "get information on issues affecting your use of the service and details of new features added"
    val bullet2 = "have access to a dedicated telephone support team"
    val bullet3 = "not get penalties during this period for missed quarterly updates this tax year"
    val insert = s"Your client must still submit their Self Assessment tax return for ${taxYearEnd - 1} to $taxYearEnd as normal."
    val subheading: String = s"Do you want to sign up this client early for the current tax year, ${taxYearEnd - 1} to $taxYearEnd?"
    val currentYearOption = "Yes"
    val currentYearOptionHint = "You will need to make sure that you use software that works with Making Tax Digital for Income Tax to send any missed quarterly updates for the year so far"
    val nextYearOption = "No"
    val continue: String = "Continue"
    val signUpAnotherClient = "Or you can check if you can sign up another client. We will not save the details you entered about FirstName LastName."
  }
}
