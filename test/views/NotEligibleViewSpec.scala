/*
 * Copyright 2017 HM Revenue & Customs
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

package views

import assets.MessageLookup.{Base => common, Not_Eligible => messages}
import forms.NotEligibleForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class NotEligibleViewSpec extends UnitTestTrait {

  lazy val page = views.html.not_eligible(
    notEligibleForm = NotEligibleForm.notEligibleForm,
    postAction = controllers.routes.NotEligibleController.submitNotEligible()
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Not Eligible view" should {

    s"have the title '${messages.title}'" in {
      document.title() must be(messages.title)
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() must be(messages.heading)
    }

    s"have the paragraph 1 (P) '${messages.line_1}'" in {
      document.getElementsByTag("P").text() must include(messages.line_1)
    }

    s"have the paragraph 2 (P) '${messages.line_2}'" in {
      document.getElementsByTag("P").text() must include(messages.line_2)
    }

    s"have the paragraph 3 (P) '${messages.line_3}'" in {
      document.getElementsByTag("P").text() must include(messages.line_3)
    }

    "have a form" which {

      val radioName = "choice"

      s"has a fieldset for Sign up and Sign out" which {
        s"has a legend which is visually hidden with the text '${messages.question}'" in {
          document.select("fieldset legend").text() mustBe messages.question
        }

        s"has a radio option for 'incomeType-${NotEligibleForm.option_signup}'" in {
          val cashRadio = document.select(s"#$radioName-${NotEligibleForm.option_signup}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe s"$radioName"
          cashRadio.attr("value") mustBe NotEligibleForm.option_signup
          val label = document.getElementsByAttributeValue("for", s"$radioName-${NotEligibleForm.option_signup}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.signUp
        }

        s"has a radio option for 'incomeType-${NotEligibleForm.option_signout}'" in {
          val cashRadio = document.select(s"#$radioName-${NotEligibleForm.option_signout}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe s"$radioName"
          cashRadio.attr("value") mustBe NotEligibleForm.option_signout
          val label = document.getElementsByAttributeValue("for", s"$radioName-${NotEligibleForm.option_signout}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.signOut
        }

      }

      s"has a post action to '${controllers.routes.NotEligibleController.submitNotEligible().url}'" in {
        document.select("form").attr("method") mustBe "POST"
        document.select("form").attr("action") mustBe controllers.routes.NotEligibleController.submitNotEligible().url
      }

      "has a continue button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe common.continue
      }

    }
  }
}
