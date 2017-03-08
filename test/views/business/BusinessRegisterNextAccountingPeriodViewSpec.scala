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

import assets.MessageLookup.Business.{RegisterNextAccountingPeriod => messages}
import forms.RegisterNextAccountingPeriodForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class BusinessRegisterNextAccountingPeriodViewSpec extends UnitTestTrait {

  lazy val backUrl = "BackUrl"

  lazy val page = views.html.business.register_next_accounting_period(
    registerNextAccountingPeriodForm = RegisterNextAccountingPeriodForm.registerNextAccountingPeriodForm,
    postAction = controllers.business.routes.RegisterNextAccountingPeriodController.submit(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  lazy val document = Jsoup.parse(page.body)

  "The 'Register Next Accounting Period' view" should {

    s"have a back button pointed to $backUrl" in {
      val backLink = document.select("#back")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    s"have the line 1 (P) '${messages.line_1}'" in {
      document.select("p").text() must include(messages.line_1)
    }

    s"have the line 2 (P) '${messages.line_2}'" in {
      document.select("p").text() must include(messages.line_2)
    }

    "has a form" which {

      s"has a fieldset for yes and no" which {

        val fieldName = RegisterNextAccountingPeriodForm.registerNextAccountingPeriod

        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for '$fieldName-${RegisterNextAccountingPeriodForm.option_yes}'" in {
          val selectedRadio = document.select(s"#$fieldName-${RegisterNextAccountingPeriodForm.option_yes}")
          selectedRadio.attr("type") mustBe "radio"
          selectedRadio.attr("name") mustBe fieldName
          selectedRadio.attr("value") mustBe RegisterNextAccountingPeriodForm.option_yes
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${RegisterNextAccountingPeriodForm.option_yes}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.yes
        }

        s"has a radio option for '$fieldName-${RegisterNextAccountingPeriodForm.option_no}'" in {
          val selectedRadio = document.select(s"#$fieldName-${RegisterNextAccountingPeriodForm.option_no}")
          selectedRadio.attr("type") mustBe "radio"
          selectedRadio.attr("name") mustBe fieldName
          selectedRadio.attr("value") mustBe RegisterNextAccountingPeriodForm.option_no
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${RegisterNextAccountingPeriodForm.option_no}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.no
        }
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.business.routes.RegisterNextAccountingPeriodController.submit().url}'" in {
        document.select("form").attr("action") mustBe controllers.business.routes.RegisterNextAccountingPeriodController.submit().url
        document.select("form").attr("method") mustBe "POST"
      }

    }

  }
}
