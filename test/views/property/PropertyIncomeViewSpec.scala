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

package views.property

import assets.MessageLookup.Property.{Income => messages}
import forms.PropertyIncomeForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class PropertyIncomeViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  lazy val page = views.html.property.property_income(
    propertyIncomeForm = PropertyIncomeForm.propertyIncomeForm,
    postAction = controllers.property.routes.PropertyIncomeController.submitPropertyIncome(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)

  lazy val document = Jsoup.parse(page.body)

  "The Property Income view" should {

    s"have a back buttong pointed to $backUrl" in {
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

    "has a form" which {

      s"has a fieldset for Business, Property and Both" which {

        val fieldName = PropertyIncomeForm.incomeValue

        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for '$fieldName-${PropertyIncomeForm.option_LT10k}'" in {
          val cashRadio = document.select(s"#$fieldName-${PropertyIncomeForm.option_LT10k}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe PropertyIncomeForm.option_LT10k
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${PropertyIncomeForm.option_LT10k}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.lt10k
        }

        s"has a radio option for '$fieldName-${PropertyIncomeForm.option_GE10k}'" in {
          val cashRadio = document.select(s"#$fieldName-${PropertyIncomeForm.option_GE10k}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe fieldName
          cashRadio.attr("value") mustBe PropertyIncomeForm.option_GE10k
          val label = document.getElementsByAttributeValue("for", s"$fieldName-${PropertyIncomeForm.option_GE10k}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.ge10k
        }
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.property.routes.PropertyIncomeController.submitPropertyIncome().url}'" in {
        document.select("form").attr("action") mustBe controllers.property.routes.PropertyIncomeController.submitPropertyIncome().url
        document.select("form").attr("method") mustBe "POST"
      }

    }

  }
}
