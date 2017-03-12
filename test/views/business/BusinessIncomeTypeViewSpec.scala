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

import assets.MessageLookup.{BusinessIncomeType => messages}
import assets.MessageLookup.Base
import forms.IncomeTypeForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class BusinessIncomeTypeViewSpec extends UnitTestTrait {
  lazy val backUrl = controllers.business.routes.BusinessNameController.showBusinessName().url

  def page(isEditMode: Boolean) = views.html.business.income_type(
    incomeTypeForm = IncomeTypeForm.incomeTypeForm,
    postAction = controllers.business.routes.BusinessIncomeTypeController.submitBusinessIncomeType(),
    backUrl = backUrl,
    isEditMode
  )(FakeRequest(), applicationMessages, appConfig)
  def documentCore(isEditMode: Boolean) = Jsoup.parse(page(isEditMode).body)

  "The Business Income Type view" should {

    lazy val document = documentCore(isEditMode = false)

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

    s"have the line_1 (P) '${messages.line_1}'" in {
      document.select("p").text() must include(messages.line_1)
    }

    "has a form" which {

      s"has a fieldset for Cash and Accruals" which {

        s"has a legend which is visually hidden with the text '${messages.heading}'" in {
          document.select("fieldset legend").text() mustBe messages.heading
        }

        s"has a radio option for 'incomeType-${IncomeTypeForm.option_cash}'" in {
          val cashRadio = document.select(s"#incomeType-${IncomeTypeForm.option_cash}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe "incomeType"
          cashRadio.attr("value") mustBe IncomeTypeForm.option_cash
          val label = document.getElementsByAttributeValue("for", s"incomeType-${IncomeTypeForm.option_cash}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.cash
        }

        s"has a radio option for 'incomeType-${IncomeTypeForm.option_accruals}'" in {
          val cashRadio = document.select(s"#incomeType-${IncomeTypeForm.option_accruals}")
          cashRadio.attr("type") mustBe "radio"
          cashRadio.attr("name") mustBe "incomeType"
          cashRadio.attr("value") mustBe IncomeTypeForm.option_accruals
          val label = document.getElementsByAttributeValue("for", s"incomeType-${IncomeTypeForm.option_accruals}")
          label.size() mustBe 1
          label.get(0).text() mustBe messages.accruals
        }
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.business.routes.BusinessIncomeTypeController.submitBusinessIncomeType().url}'" in {
        document.select("form").attr("action") mustBe controllers.business.routes.BusinessIncomeTypeController.submitBusinessIncomeType().url
        document.select("form").attr("method") mustBe "POST"
      }

      "say update" in {
        lazy val documentEdit = documentCore(isEditMode = true)
        documentEdit.select("#continue-button").text() mustBe Base.update
      }

    }

  }
}
