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

package views.business

import assets.MessageLookup.Business.{SoleTrader => messages}
import forms.SoleTraderForm
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class SoleTraderViewSpec extends PlaySpec with OneAppPerTest {

  lazy val page = views.html.business.sole_trader(
    soleTraderForm = SoleTraderForm.soleTraderForm,
    postAction = controllers.business.routes.SoleTraderController.submitSoleTrader()
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Sole trader view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    s"have the line 1 (P) '${messages.line_1}'" in {
      document.select("p").text() must include(messages.line_1)
    }

    "has a form" which {

      s"has a fieldset for yes and no" which {

        val fieldName = SoleTraderForm.soleTrader

        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for '$fieldName-${SoleTraderForm.option_yes}'" in {
          val cashRadio = document.select(s"#$fieldName-${SoleTraderForm.option_yes}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe SoleTraderForm.option_yes
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${SoleTraderForm.option_yes}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.yes
        }

        s"has a radio option for '$fieldName-${SoleTraderForm.option_no}'" in {
          val cashRadio = document.select(s"#$fieldName-${SoleTraderForm.option_no}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe SoleTraderForm.option_no
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${SoleTraderForm.option_no}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.no
        }
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.business.routes.SoleTraderController.submitSoleTrader().url}'" in {
        document.select("form").attr("action") mustBe controllers.business.routes.SoleTraderController.submitSoleTrader().url
        document.select("form").attr("method") mustBe "POST"
      }

    }

  }
}
