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

import config.featureswitch.FeatureSwitch.TaxYear26To27Plus
import forms.individual.business.AccountingYearForm
import messagelookup.agent.MessageLookup
import models.{Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.FormError
import play.twirl.api.Html
import services.AccountingPeriodService
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.NextYearMandatorySignUp

class NextYearMandatorySignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(TaxYear26To27Plus)
  }

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]

  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  val nextYearMandatorySignUp: NextYearMandatorySignUp = app.injector.instanceOf[NextYearMandatorySignUp]

  val testFormError: FormError = FormError(AccountingYearForm.accountingYear, "error.business.what-year-to-sign-up.empty")

  "what year to sign up" must {
    "have the correct template details" when {
      "the page has no back link" in new TemplateViewTest(
        view = page(editMode = false),
        title = NextYearMandatorySignUp.heading,
      )
      "the page has a back link + error" in new TemplateViewTest(
        view = page(editMode = false, hasError = true),
        title = NextYearMandatorySignUp.heading,
        backLink = Some(testBackUrl),
        error = Some(testFormError)
      )
    }

    "have a heading" in {
      document().select("h1").text mustBe NextYearMandatorySignUp.heading
    }

    "has a first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe NextYearMandatorySignUp.paragraph
    }

    "has a second paragraph" which {
      "leads into the bullet list" when {
        "the 26-27 feature switch is disabled" in {
          document().mainContent.selectNth("p", 2).text mustBe NextYearMandatorySignUp.paragraph2Pre2627
        }
      }
      "is an independent paragraph" when {
        "the 26-27 feature switch is enabled" in {
          enable(TaxYear26To27Plus)

          document().mainContent.selectNth("p", 2).text mustBe NextYearMandatorySignUp.paragraph2Post2627
        }
      }
    }

    "have a bullet list" when {
      "the 26-27 feature switch is disabled" in {
        def bulletList: Element = document().mainContent.selectNth("ul", 1)

        bulletList.selectNth("li", 1).text mustBe NextYearMandatorySignUp.bullet1
        bulletList.selectNth("li", 2).text mustBe NextYearMandatorySignUp.bullet2
        bulletList.selectNth("li", 3).text mustBe NextYearMandatorySignUp.bullet3
      }
    }

    "don't have a bullet list" when {
      "the 26-27 feature switch is enabled" in {
        enable(TaxYear26To27Plus)

        document().mainContent.selectOptionalNth("ul", 1) mustBe None
      }
    }

    "have a subheading" in {
      document().mainContent.selectNth("h2", 1).text mustBe NextYearMandatorySignUp.subheading
    }

    "has a third paragraph" in {
      document().mainContent.selectNth("p", 3).text mustBe NextYearMandatorySignUp.paragraph3
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
          legend = NextYearMandatorySignUp.heading,
          isHeading = false,
          isLegendHidden = true,
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(NextYearMandatorySignUp.currentYearOption),
              value = Some(Current.toString)
            ),
            RadioItem(
              content = Text(NextYearMandatorySignUp.nextYearOption),
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
    nextYearMandatorySignUp(
      if (hasError) AccountingYearForm.accountingYearForm.withError(testFormError) else AccountingYearForm.accountingYearForm,
      postAction = testCall,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )

  private def document(editMode: Boolean = false, hasError: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode, hasError).body)

  object NextYearMandatorySignUp {
    val heading: String = s"You must use Making Tax Digital for Income Tax next tax year, $taxYearEnd to ${taxYearEnd + 1}"
    val paragraph: String = s"You must use Making Tax Digital for Income Tax next tax year to submit your $taxYearEnd to ${taxYearEnd + 1} income."
    val paragraph2Pre2627 = "But you can choose to sign up early, so you are prepared to use the service. This will mean that you will:"
    val paragraph2Post2627 = "But you can choose to sign up early, so you are prepared to use the service."
    val bullet1 = "get information by email on issues affecting your use of the service and details of new features added"
    val bullet2 = "have access to a dedicated telephone support team"
    val bullet3: String = s"not get penalties during this period for any missed quarterly updates before 6 April $taxYearEnd"
    val subheading = "When do you want to start using Making Tax Digital for Income Tax?"
    val paragraph3 = "I want to sign up now, and:"
    val currentYearOption: String = s"start using it now for this tax year, ${taxYearEnd - 1} to $taxYearEnd"
    val nextYearOption: String = s"use it from next tax year, $taxYearEnd to ${taxYearEnd + 1}"
  }
}
