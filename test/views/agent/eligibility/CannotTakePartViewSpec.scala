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

package views.agent.eligibility

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import utilities.ViewSpec
import views.html.agent.eligibility.CannotTakePart

class CannotTakePartViewSpec extends ViewSpec {
  private val view = app.injector.instanceOf[CannotTakePart]


  "Cannot Sign Up View" should {
    "have a title" in {
      document.title mustBe s"${CannotTakePartMessages.heading} - Use software to report your client’s Income Tax - GOV.UK"
    }

    "have a heading" in {
      document.mainContent.select("h1").text() mustBe CannotTakePartMessages.heading
    }

    "have paragraph 1" in {
      document.mainContent.selectNth("p", 1).text() mustBe CannotTakePartMessages.paragraph1
    }

    "have paragraph 2" in {
      document.mainContent.selectNth("p", 2).text() mustBe CannotTakePartMessages.paragraph2
    }

    "have link 1" in {
      val link = document.mainContent.selectNth("a", 1)
      link.text() mustBe CannotTakePartMessages.link1
      link.attr("href") mustBe "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax"
    }


    "have inset 1" in {
      document.mainContent.selectNth("p", 3).text() mustBe CannotTakePartMessages.inset1
    }

    "have a sign up another client link" in {
      val link = document.mainContent.selectHead(".govuk-button")
      link.text() mustBe CannotTakePartMessages.signUpAnotherClientLink
    }
  }

  private def document = Jsoup.parse(view().body)

  object CannotTakePartMessages {
    val heading = "Your client cannot take part in this pilot yet"
    val paragraph1 = "The Making Tax Digital For Income Tax pilot is not currently available to people with certain incomes or deductions."
    val paragraph2 = "However, your client may be able to sign up to Making Tax Digital for Income Tax in the future. Learn more about who’s eligible for Making Tax Digital for Income Tax."
    val link1 = "who’s eligible for Making Tax Digital for Income Tax"
    val inset1 = "Your client’s Self Assessment tax return must be submitted as normal."
    val signUpAnotherClientLink = "Sign up another client"
  }
}
