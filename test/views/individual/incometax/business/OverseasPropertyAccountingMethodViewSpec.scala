/*
 * Copyright 2020 HM Revenue & Customs
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

package views.individual.incometax.business

import assets.MessageLookup.{Base => common, OverseasPropertyAccountingMethod => messages}
import forms.individual.business.AccountingMethodOverseasPropertyForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class OverseasPropertyAccountingMethodViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall

  implicit val request: Request[_] = FakeRequest()

  class Setup(isEditMode: Boolean = false) {
    val page: HtmlFormat.Appendable = views.html.individual.incometax.business.overseas_property_accounting_method(
      overseasPropertyAccountingMethodForm = AccountingMethodOverseasPropertyForm.accountingMethodOverseasPropertyForm,
      postAction = action,
      isEditMode,
      backUrl = backUrl
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "property accounting method" must {

    "have a title" in new Setup {
      document.title mustBe messages.title
    }

    "have a heading" in new Setup {
      document.select("h1").text mustBe messages.heading
    }

    "have a back button" in new Setup {
      val backButton: Elements = document.select(".back-link")
      backButton.attr("href") mustBe backUrl
      backButton.text mustBe common.back
    }

    "have a accordion" which {
      "has a summary" in new Setup {
        document.select("details summary").text mustBe messages.accordionSummary
      }
      "has content" in new Setup {
        document.select("details div p").text mustBe messages.accordionContentPara
        document.select("details ul li").text mustBe Seq(
          messages.accordionContentBullet1,
          messages.accordionContentBullet2
        ).mkString(" ")
      }
    }

    "have a form" which {
      "has correct attributes" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe action.method
        form.attr("action") mustBe action.url
      }

      "has a cash radio button" in new Setup {
        val radioWithLabel: Elements = document.select("form div fieldset div.multiple-choice")
        radioWithLabel.select("input[id=accountingMethodOverseasProperty-Cash]").`val` mustBe "Cash"
        radioWithLabel.select("label[for=accountingMethodOverseasProperty-Cash]").text mustBe Seq(
          messages.radioCash,
          messages.radioCashDetail
        ).mkString(" ")
      }

      "has a accruals radio button" in new Setup {
        val radioWithLabel: Elements = document.select("form div fieldset div.multiple-choice")
        radioWithLabel.select("input[id=accountingMethodOverseasProperty-Accruals]").`val` mustBe "Accruals"
        radioWithLabel.select("label[for=accountingMethodOverseasProperty-Accruals]").text mustBe Seq(
          messages.radioAccruals,
          messages.radioAccrualsDetail
        ).mkString(" ")
      }

      "has a continue button" that {
        s"displays ${common.continue} when not in edit mode" in new Setup {
          document.select("button[type=submit]").text mustBe common.continue
        }
        s"displays ${common.update} when in edit mode" in new Setup(isEditMode = true) {
          document.select("button[type=submit]").text mustBe common.update
        }
      }
    }

  }


}
