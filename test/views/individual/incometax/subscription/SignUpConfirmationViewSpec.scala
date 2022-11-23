/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.incometax.subscription

import models.{AccountingYear, Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.incometax.subscription.SignUpConfirmation

class SignUpConfirmationViewSpec extends ViewSpec {
  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  def page(selectedTaxYear: Option[AccountingYear]): Html = signUpConfirmation(selectedTaxYear)

  def document(selectedTaxYear: Option[AccountingYear] = Some(Current)): Document = Jsoup.parse(page(selectedTaxYear).body)

  "The sign up confirmation view" must {
    "have a heading" in {
      document().mainContent.selectHead("h1").text() mustBe SignUpConfirmationMessages.heading
    }

    "have a section 1" which {
      "contains a heading" in {
        document().mainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.section1heading
      }

      "not contains a hint" when {
        "the Current tax year is selected" in {
          document().mainContent.select(".govuk-warning-text .govuk-warning-text__text").isEmpty mustBe true
        }
      }

      "contains a hint" when {
        "the Next tax year is selected" in {
          document(Some(Next)).mainContent.selectHead(".govuk-warning-text .govuk-warning-text__text").text() mustBe SignUpConfirmationMessages.section1hint
        }
      }
    }



    "have a section 2" which {
      "contains a heading" in {
        document().mainContent.selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.section2heading
      }
    }
  }

  private object SignUpConfirmationMessages {
    val heading = "Sign up complete"
    val section1heading = "What you will have to do"
    val section1hint = "Warning Continue to submit your Self Assessment tax return, as normal, until 2024."
    val section2heading = "Find software and check your account"
  }
}
