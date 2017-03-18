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

import assets.MessageLookup.{Base => common, OtherIncome => messages}
import forms.OtherIncomeForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class OtherIncomeViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  lazy val page = views.html.other_income(
    otherIncomeForm = OtherIncomeForm.otherIncomeForm,
    postAction = controllers.routes.OtherIncomeController.submitOtherIncome(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  lazy val document = Jsoup.parse(page.body)

  "The Other Income View" should {

    s"have a back button pointed to $backUrl" in {
      val backLink = document.select("#back")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document.title() must be(messages.title)
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() must be(messages.heading)
    }

    s"have the paragraph 1 (P) '${messages.para1}'" in {
      document.getElementsByTag("P").text() must include(messages.para1)
    }

    s"have the paragraph (LI) '${messages.bullet1}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet1)
    }

    s"have the paragraph (LI) '${messages.bullet2}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet2)
    }

    s"have the paragraph (LI) '${messages.bullet3}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet3)
    }

    s"have the paragraph (LI) '${messages.bullet4}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet4)
    }

    s"have the paragraph (LI) '${messages.bullet5}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet5)
    }

    s"have the paragraph (LI) '${messages.bullet6}'" in {
      document.getElementsByTag("LI").text() must include (messages.bullet6)
    }

    "have a form" which {

      val radioName = "choice"

      s"has a fieldset for yes and no" which {
        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for '$radioName-${OtherIncomeForm.option_yes}'" in {
          val cashRadio = document.select(s"#$radioName-${OtherIncomeForm.option_yes}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe s"$radioName"
          cashRadio.attr("value") mustBe OtherIncomeForm.option_yes
          val label = document.getElementsByAttributeValue("for", s"$radioName-${OtherIncomeForm.option_yes}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.yes
        }

        s"has a radio option for '$radioName-${OtherIncomeForm.option_no}'" in {
          val cashRadio = document.select(s"#$radioName-${OtherIncomeForm.option_no}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe s"$radioName"
          cashRadio.attr("value") mustBe OtherIncomeForm.option_no
          val label = document.getElementsByAttributeValue("for", s"$radioName-${OtherIncomeForm.option_no}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.no

        }

        s"has a post action to '${controllers.routes.OtherIncomeController.submitOtherIncome().url}'" in {
          document.select("form").attr("method") mustBe "POST"
          document.select("form").attr("action") mustBe controllers.routes.OtherIncomeController.submitOtherIncome().url
        }

        "has a continue button" in {
          document.select("button").attr("type") mustBe "submit"
          document.select("button").text() mustBe common.continue
        }
      }
    }
}}
