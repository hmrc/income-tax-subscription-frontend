/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent.business

import agent.assets.MessageLookup
import forms.agent.AccountingYearForm
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
    val page: HtmlFormat.Appendable = views.html.agent.business.what_year_to_sign_up(
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
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      document.title mustBe MessageLookup.Business.WhatYearToSignUp.heading + serviceNameGovUk
    }

    "have a heading" in new Setup {
      document.select("h1").text mustBe MessageLookup.Business.WhatYearToSignUp.heading
    }

    "have content" in new Setup {
      val paragraphs: Elements = document.select(".content__body").select("p")
      val uls: Elements = document.select(".content__body").select("ul").select("li")
      paragraphs.get(0).text() mustBe MessageLookup.Business.WhatYearToSignUp.line1
      paragraphs.get(1).text() mustBe MessageLookup.Business.WhatYearToSignUp.option1ConditionalExample1
      paragraphs.get(2).text() mustBe MessageLookup.Business.WhatYearToSignUp.option1ConditionalExample2((taxYearEnd + 1).toString)
      paragraphs.get(3).text() mustBe MessageLookup.Business.WhatYearToSignUp.option2ConditionalExample1
      paragraphs.get(4).text() mustBe MessageLookup.Business.WhatYearToSignUp.option2ConditionalExample2((taxYearEnd + 2).toString)

      uls.get(0).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate1((taxYearEnd - 1).toString)
      uls.get(1).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate2((taxYearEnd - 1).toString)
      uls.get(2).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate3(taxYearEnd.toString)
      uls.get(3).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate4(taxYearEnd.toString)

      uls.get(4).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate1(taxYearEnd.toString)
      uls.get(5).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate2(taxYearEnd.toString)
      uls.get(6).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate3((taxYearEnd + 1).toString)
      uls.get(7).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate4((taxYearEnd + 1).toString)
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
          MessageLookup.Business.WhatYearToSignUp.option1((taxYearEnd - 1).toString, taxYearEnd.toString)
        ).mkString(" ")
      }

      "has a next tax year radio button" in new Setup {
        val radioWithLabel: Elements = document.select("form fieldset div div.multiple-choice")
        radioWithLabel.select("input[id=accountingYear-2]").`val` mustBe "NextYear"
        radioWithLabel.select("label[for=accountingYear-2]").text mustBe Seq(
          MessageLookup.Business.WhatYearToSignUp.option2(taxYearEnd.toString, (taxYearEnd + 1).toString)
        ).mkString(" ")
      }

      "has a continue button" that {
        s"displays ${MessageLookup.Base.continue} when not in edit mode" in new Setup {
          document.select("button[type=submit]").text mustBe MessageLookup.Base.continue
        }
        s"displays ${MessageLookup.Base.update} when in edit mode" in new Setup(isEditMode = true) {
          document.select("button[type=submit]").text mustBe MessageLookup.Base.update
        }
      }
    }

    "have a back button" when {
      "in edit mode" in new Setup(isEditMode = true) {
        val backButton: Elements = document.select(".link-back")
        backButton.attr("href") mustBe backUrl
        backButton.text mustBe MessageLookup.Base.back
      }
    }
    "not have a back button" when {
      "not in edit mode" in new Setup(isEditMode = false) {
        Option(document.selectFirst(".link-back")) mustBe None
      }
    }
  }
}
