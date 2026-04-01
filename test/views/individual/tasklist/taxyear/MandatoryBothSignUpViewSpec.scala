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

package views.individual.tasklist.taxyear

import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.individual.tasklist.taxyear.MandatoryBothSignUp

class MandatoryBothSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  val mandatoryBothSignUp: MandatoryBothSignUp = app.injector.instanceOf[MandatoryBothSignUp]

  "Tax Year Selection Mandatory Both" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(editMode = false),
      isAgent = false,
      title = MandatoryBothSignUpMessages.heading
    )

    "have a heading" in {
      document().select("h1").text mustBe MandatoryBothSignUpMessages.heading
    }

    "have the paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe MandatoryBothSignUpMessages.paragraph
    }

    "has a continue button" in {
      document().select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
    }
  }

  private object MandatoryBothSignUpMessages {
    val heading = "You must use Making Tax Digital for Income Tax now"
    val paragraph: String = s"As your total annual income from self-employment or property is over £50,000, you must use Making Tax Digital for Income Tax from $taxYearEnd to ${taxYearNext}."
  }

  private def page(editMode: Boolean): Html = {
    mandatoryBothSignUp(
      postAction = testCall,
      endYearOfCurrentTaxPeriod = taxYearEnd
    )
  }

  private def document(editMode: Boolean = false): Document =
    Jsoup.parse(page(editMode = editMode).body)

}
