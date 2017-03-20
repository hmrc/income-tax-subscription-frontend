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

package views.preferences

import assets.MessageLookup
import assets.MessageLookup.{PreferencesCallBack => messages}
import forms.preferences.BackToPreferencesForm._
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import utils.UnitTestTrait


class ContinueRegistrationViewSpec extends UnitTestTrait {
  lazy val page = views.html.preferences.continue_registration(
    backToPreferencesForm,
    postAction = controllers.preferences.routes.PreferencesController.submitGoBackToPreferences()
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Continue Registration view" should {

    s"have the title '${messages.title}'" in {
      document.title() must be(messages.title)
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() must be(messages.heading)
    }

    s"have the line_1 (P) '${messages.line_1}'" in {
      document.getElementsByTag("p").text() must include(messages.line_1)
    }

    "have a form" which {

      s"has a post action to '${controllers.preferences.routes.PreferencesController.submitGoBackToPreferences().url}'" in {
        document.select("form").attr("method") mustBe "POST"
        document.select("form").attr("action") mustBe controllers.preferences.routes.PreferencesController.submitGoBackToPreferences().url
      }

      val fieldName = backToPreferences

      s"has a legend which is visually hidden with the text '${messages.legend}'" in {
        document.select("fieldset legend").text() mustBe messages.legend
      }

      s"has a radio option for '$fieldName-$option_yes'" in {
        val cashRadio = document.select(s"#$fieldName-$option_yes")
        cashRadio.attr("type") mustBe "radio"
        cashRadio.attr("name") mustBe fieldName
        cashRadio.attr("value") mustBe option_yes
        val label = document.getElementsByAttributeValue("for", s"$fieldName-$option_yes")
        label.size() mustBe 1
        label.get(0).text() mustBe messages.yes
      }

      s"has a radio option for '$fieldName-$option_no'" in {
        val cashRadio = document.select(s"#$fieldName-$option_no")
        cashRadio.attr("type") mustBe "radio"
        cashRadio.attr("name") mustBe fieldName
        cashRadio.attr("value") mustBe option_no
        val label = document.getElementsByAttributeValue("for", s"$fieldName-$option_no")
        label.size() mustBe 1
        label.get(0).text() mustBe messages.no
      }

      "has a continue button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe MessageLookup.Base.continue
      }

    }
  }
}
