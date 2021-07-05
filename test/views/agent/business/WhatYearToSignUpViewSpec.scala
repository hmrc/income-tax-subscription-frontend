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
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.agent.business.WhatYearToSignUp

class WhatYearToSignUpViewSpec extends ViewSpec {

  val backUrl: String = ViewSpecTrait.testBackUrl
  val action: Call = ViewSpecTrait.testCall
  val taxYearEnd: Int = 2020

  val whatYearToSignUp: WhatYearToSignUp = app.injector.instanceOf[WhatYearToSignUp]

  def view(editMode: Boolean = false): Html = {
    whatYearToSignUp(
      AccountingYearForm.accountingYearForm,
      postAction = testCall,
      backUrl = testBackUrl,
      endYearOfCurrentTaxPeriod = taxYearEnd,
      isEditMode = editMode,
    )
  }

    class ViewTest(editMode: Boolean = false) {
      val document: Document = Jsoup.parse(view(editMode = editMode).body)
    }

  "what year to sign up" must {
    "have a title" in new ViewTest {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      document.title mustBe MessageLookup.Business.WhatYearToSignUp.heading + serviceNameGovUk
    }

    "have a heading" in new ViewTest {
      document.select("h1").text mustBe MessageLookup.Business.WhatYearToSignUp.heading
    }

    "have content" in new ViewTest {
      val paragraphs: Elements = document.select(".govuk-body").select("p")
      val conditionalListLabels: Elements = document.select(".govuk-radios__conditional").select(".govuk-list").select("li")
      document.select(".govuk-hint").get(0).text() mustBe MessageLookup.Business.WhatYearToSignUp.line1
      paragraphs.get(0).text() mustBe MessageLookup.Business.WhatYearToSignUp.option1ConditionalExample1
      paragraphs.get(1).text() mustBe MessageLookup.Business.WhatYearToSignUp.option1ConditionalExample2((taxYearEnd + 1).toString)
      paragraphs.get(2).text() mustBe MessageLookup.Business.WhatYearToSignUp.option2ConditionalExample1
      paragraphs.get(3).text() mustBe MessageLookup.Business.WhatYearToSignUp.option2ConditionalExample2((taxYearEnd + 2).toString)

      conditionalListLabels.get(0).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate1((taxYearEnd - 1).toString)
      conditionalListLabels.get(1).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate2((taxYearEnd - 1).toString)
      conditionalListLabels.get(2).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate3(taxYearEnd.toString)
      conditionalListLabels.get(3).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate4(taxYearEnd.toString)

      conditionalListLabels.get(4).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate1(taxYearEnd.toString)
      conditionalListLabels.get(5).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate2(taxYearEnd.toString)
      conditionalListLabels.get(6).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate3((taxYearEnd + 1).toString)
      conditionalListLabels.get(7).text() mustBe MessageLookup.Business.WhatYearToSignUp.conditionalDate4((taxYearEnd + 1).toString)
    }

    "have a form" which {
      "has correct attributes" in new ViewTest {
        val form: Elements = document.select("form")
        form.attr("method") mustBe action.method
        form.attr("action") mustBe action.url
      }

      "has a current tax year radio button" in new ViewTest {
        val radio: Element = document.select(".govuk-radios__item").get(0)
        radio.select("input[id=accountingYear]").`val` mustBe "CurrentYear"
        radio.select("label[for=accountingYear]").text mustBe Seq(
          MessageLookup.Business.WhatYearToSignUp.option1((taxYearEnd - 1).toString, taxYearEnd.toString)
        ).mkString(" ")
      }

      "has a next tax year radio button" in new ViewTest {
        val radio: Element = document.select(".govuk-radios__item").get(1)
        radio.select("input[id=accountingYear-2]").`val` mustBe "NextYear"
        radio.select("label[for=accountingYear-2]").text mustBe Seq(
          MessageLookup.Business.WhatYearToSignUp.option2(taxYearEnd.toString, (taxYearEnd + 1).toString)
        ).mkString(" ")
      }

      "has a continue button" that {
        s"displays ${MessageLookup.Base.continue} when not in edit mode" in new ViewTest {
          document.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
        s"displays ${MessageLookup.Base.update} when in edit mode" in new ViewTest(editMode = true) {
          document.select("button[id=continue-button]").text mustBe MessageLookup.Base.update
        }
      }
    }

    "have a back button" when {
      "in edit mode" in new ViewTest(editMode = true) {
        val backButton: Elements = document.select(".govuk-back-link")
        backButton.attr("href") mustBe backUrl
        backButton.text mustBe MessageLookup.Base.back
      }
    }
    "not have a back button" when {
      "not in edit mode" in new ViewTest() {
        Option(document.selectFirst(".link-back")) mustBe None
      }
    }
  }
}
