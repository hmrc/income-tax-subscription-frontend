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

import forms.individual.accountingperiod.AccountingPeriodForm
import models.common.BusinessAccountingPeriod
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import utilities.ViewSpec
import views.html.individual.accountingPeriod.AccountingPeriod


class AccountingPeriodViewSpec extends ViewSpec {

  private val accountingPeriod: AccountingPeriod = app.injector.instanceOf[AccountingPeriod]

  "AccountingPeriod" must {
    "have the correct template details" when {
      "the page has no error" in new TemplateViewTest(
        view = page(),
        isAgent = false,
        title = AccountingPeriodMessages.heading,
        backLink = Some(controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
      )

      "the page has an error" in new TemplateViewTest(
        view = page(hasError = true),
        isAgent = false,
        title = AccountingPeriodMessages.heading,
        error = Some(testFormError),
        backLink = Some(controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
      )
    }
    "have a heading" in {
      document().getH1Element.text mustBe AccountingPeriodMessages.heading
    }

    "have the correct first paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe AccountingPeriodMessages.paraOne
    }

    "have the correct second paragraph" in {
      document().mainContent.selectNth("p", 2).text mustBe AccountingPeriodMessages.paraTwo
    }

    "have a form" which {
      def form: Element = document().selectHead("form")

      "has correct attributes" in {
        form.attr("method") mustBe testCall.method
        form.attr("action") mustBe testCall.url
      }

      "has the correct radio inputs" in {
        form.mustHaveRadioInput(selector = "fieldset")(
          name = "accountingPeriod",
          legend = AccountingPeriodMessages.legend,
          isHeading = false,
          isLegendHidden = false,
          headingClasses = Some("govuk-fieldset__legend--m"),
          hint = None,
          errorMessage = None,
          radioContents = Seq(
            RadioItem(
              content = Text(AccountingPeriodMessages.sixthToFifth),
              value = Some(BusinessAccountingPeriod.SixthAprilToFifthApril.key),
              hint = Some(Hint(content = Text(AccountingPeriodMessages.sixthToFifthHint))),
            ),
            RadioItem(
              content = Text(AccountingPeriodMessages.firstToThirtyFirst),
              value = Some(BusinessAccountingPeriod.FirstAprilToThirtyFirstMarch.key),
              hint = Some(Hint(content = Text(AccountingPeriodMessages.firstToThirtyFirstHint))),
            )
          )
        )
      }

      "has a continue button" in {
        form.select("button[id=continue-button]").text mustBe AccountingPeriodMessages.continue
      }
    }
  }

  private val testFormError: FormError = FormError(AccountingPeriodForm.fieldName, "accounting-period.error")

  private def page(hasError: Boolean = false): Html = {
    accountingPeriod(
      if (hasError) AccountingPeriodForm.accountingPeriodForm.withError(testFormError) else AccountingPeriodForm.accountingPeriodForm,
      testCall,
      controllers.individual.tasklist.taxyear.routes.WhatYearToSignUpController.show().url
    )
  }

  private def document(hasError: Boolean = false): Document = Jsoup.parse(page(hasError).body)

  object AccountingPeriodMessages {
    val heading = "Your business accounting period"
    val paraOne = "This is the accounting period you use each year when reporting your business income and expenses to HMRC."
    val paraTwo = "Your accounting period runs from the date your books or accounts start. It ends on the date your books or accounts are made up to."
    val legend = "What accounting period do you use for your business?"
    val sixthToFifth = "6 April to 5 April"
    val sixthToFifthHint = "Your books or accounts start on 6 April and are made up to 5 April of the following year (such as, 6 April 2026 to 5 April 2027)"
    val firstToThirtyFirst = "1 April to 31 March"
    val firstToThirtyFirstHint = "Your books or accounts start on 1 April and are made up to 31 March of the following year (such as, 1 April 2026 to 31 March 2027)"
    val other = "Neither of these"
    val continue = "Continue"
  }

}