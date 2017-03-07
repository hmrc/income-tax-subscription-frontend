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

import assets.MessageLookup.{IncomeSource => messages}
import forms.IncomeSourceForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class IncomeSourceViewSpec extends UnitTestTrait {

  lazy val page = views.html.income_source(
    incomeSourceForm = IncomeSourceForm.incomeSourceForm,
    postAction = controllers.routes.IncomeSourceController.submitIncomeSource()
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Income source view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    "has a form" which {

      s"has a fieldset for Business, Property and Both" which {

        val fieldName = IncomeSourceForm.incomeSource

        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for '$fieldName-${IncomeSourceForm.option_business}'" in {
          val cashRadio = document.select(s"#$fieldName-${IncomeSourceForm.option_business}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe IncomeSourceForm.option_business
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${IncomeSourceForm.option_business}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.business
        }

        s"has a radio option for '$fieldName-${IncomeSourceForm.option_property}'" in {
          val cashRadio = document.select(s"#$fieldName-${IncomeSourceForm.option_property}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe IncomeSourceForm.option_property
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${IncomeSourceForm.option_property}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.property
        }

        s"has a radio option for '$fieldName-${IncomeSourceForm.option_both}'" in {
          val cashRadio = document.select(s"#$fieldName-${IncomeSourceForm.option_both}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe IncomeSourceForm.option_both
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${IncomeSourceForm.option_both}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.both
        }

        s"has a radio option for '$fieldName-${IncomeSourceForm.option_other}'" in {
          val cashRadio = document.select(s"#$fieldName-${IncomeSourceForm.option_other}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe IncomeSourceForm.option_other
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${IncomeSourceForm.option_other}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.other
        }
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.routes.IncomeSourceController.submitIncomeSource().url}'" in {
        document.select("form").attr("action") mustBe controllers.routes.IncomeSourceController.submitIncomeSource().url
        document.select("form").attr("method") mustBe "POST"
      }

    }

  }
}
