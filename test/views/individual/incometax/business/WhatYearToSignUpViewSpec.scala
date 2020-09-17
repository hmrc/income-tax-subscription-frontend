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

import assets.MessageLookup.{Base => common, WhatYearToSignUp => messages}
import forms.individual.business.AccountingYearForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait

class WhatYearToSignUpViewSpec extends ViewSpecTrait {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall
  val taxYearEnd: Int = 2020

  class Setup(isEditMode: Boolean = false) {
    val page: HtmlFormat.Appendable = views.html.individual.incometax.business.what_year_to_sign_up(
      accountingYearForm = AccountingYearForm.accountingYearForm,
      postAction = action,
      backUrl = backUrl,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = isEditMode
    )(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "what year to sign up" must {
    "have a title" in new Setup {
      document.title mustBe messages.title
    }

    "have a heading" in new Setup {
      document.select("h1").text mustBe messages.heading
    }

    "have content" in new Setup {
      val paragraphs: Elements = document.select(".content__body").select("p")
      val uls: Elements = document.select(".content__body").select("ul").select("li")
      paragraphs.get(0).text() mustBe messages.line1
      paragraphs.get(1).text() mustBe messages.option1ConditionalExample1
      paragraphs.get(2).text() mustBe messages.option1ConditionalExample2((taxYearEnd + 1).toString)
      paragraphs.get(3).text() mustBe messages.option2ConditionalExample1
      paragraphs.get(4).text() mustBe messages.option2ConditionalExample2(taxYearEnd.toString)

      uls.get(0).text() mustBe messages.conditionalDate1((taxYearEnd - 1).toString)
      uls.get(1).text() mustBe messages.conditionalDate2((taxYearEnd - 1).toString)
      uls.get(2).text() mustBe messages.conditionalDate3(taxYearEnd.toString)
      uls.get(3).text() mustBe messages.conditionalDate4(taxYearEnd.toString)

      uls.get(4).text() mustBe messages.conditionalDate1(taxYearEnd.toString)
      uls.get(5).text() mustBe messages.conditionalDate2(taxYearEnd.toString)
      uls.get(6).text() mustBe messages.conditionalDate3((taxYearEnd + 1).toString)
      uls.get(7).text() mustBe messages.conditionalDate4((taxYearEnd + 1).toString)

    }

    "have a form" which {
      "has correct attributes" in new Setup {
        val form: Elements = document.select("form")
        form.attr("method") mustBe action.method
        form.attr("action") mustBe action.url
      }

      "has a current tax year radio button" in new Setup {
        val radioWithLabel: Elements = document.select("form fieldset div div.multiple-choice")
        radioWithLabel.select("input[id=accountingYear]").`val` mustBe "CurrentYear"
        radioWithLabel.select("label[for=accountingYear]").text mustBe Seq(
          messages.option1((taxYearEnd - 1).toString, taxYearEnd.toString)
        ).mkString(" ")
      }

      "has a next tax year radio button" in new Setup {
        val radioWithLabel: Elements = document.select("form fieldset div div.multiple-choice")
        radioWithLabel.select("input[id=accountingYear-2]").`val` mustBe "NextYear"
        radioWithLabel.select("label[for=accountingYear-2]").text mustBe Seq(
          messages.option2(taxYearEnd.toString, (taxYearEnd + 1).toString)
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

    "have a back button" in new Setup {
      val backButton: Elements = document.select(".back-link")
      backButton.attr("href") mustBe backUrl
      backButton.text mustBe common.back
    }
  }
}
