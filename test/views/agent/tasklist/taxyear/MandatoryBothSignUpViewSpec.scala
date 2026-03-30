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

package views.agent.tasklist.taxyear

import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.ViewSpec
import views.html.agent.tasklist.taxyear.MandatoryBothSignUp
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}

class MandatoryBothSignUpViewSpec extends ViewSpec {

  private val accountingPeriodService = app.injector.instanceOf[AccountingPeriodService]
  val taxYearEnd: Int = accountingPeriodService.currentTaxYear
  val taxYearPrevious: Int = taxYearEnd - 1
  val taxYearNext: Int = taxYearEnd + 1

  private val fullName = "FirstName LastName"
  private val nino = "ZZ 11 11 11 Z"

  val mandatoryBothSignUp: MandatoryBothSignUp = app.injector.instanceOf[MandatoryBothSignUp]

  "Tax Year Selection Mandatory Both" must {

    "have the correct template details" in new TemplateViewTest(
      view = page(editMode = false, clientName = fullName, clientNino = nino),
      isAgent = true,
      title = MandatoryBothSignUpMessages.heading
    )

    "have a heading and caption" in {
      document().mainContent.mustHaveHeadingAndCaption(
        heading = MandatoryBothSignUpMessages.heading,
        caption = MandatoryBothSignUpMessages.caption,
        isSection = false
      )
    }

    "have the paragraph" in {
      document().mainContent.selectNth("p", 1).text mustBe MandatoryBothSignUpMessages.paragraph
    }

    "has a continue button" in {
      document().select("button[id=continue-button]").text mustBe MandatoryBothSignUpMessages.continue
    }

    "have the second paragraph" in {
      val paragraph: Elements = document().mainContent.select(".govuk-form-group").select(".govuk-body")
      paragraph.text must include(MandatoryBothSignUpMessages.signUpAnotherClient)
      paragraph.select("a.govuk-link").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }
  }

  private object MandatoryBothSignUpMessages {
    val heading = "Your client must use Making Tax Digital for Income Tax now"
    val caption: String = fullName + " - " + nino
    val paragraph: String = s"As your client’s total annual income from self-employment or property is over £50,000, they must use Making Tax Digital for Income Tax for the tax year ${taxYearPrevious} to $taxYearEnd and beyond."
    val signUpAnotherClient = "Or you can check if you can sign up another client. We will not save the details you entered about FirstName LastName."
    val continue: String = "Sign up this client"
  }

  private def page(editMode: Boolean, clientName: String = fullName, clientNino: String = nino): Html = {
    mandatoryBothSignUp(
      postAction = testCall,
      clientName,
      clientNino,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )
  }

  private def document(editMode: Boolean = false,
                       clientName: String = fullName,
                       clientNino: String = nino): Document =
    Jsoup.parse(page(editMode = editMode, clientName, clientNino).body)

}
