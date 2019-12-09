/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.views.business

import agent.assets.MessageLookup
import agent.forms.MatchTaxYearForm
import agent.models.MatchTaxYearModel
import agent.views.html.business.match_to_tax_year
import core.views.ViewSpecTrait
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

class MatchTaxYearViewSpec extends ViewSpecTrait {

  class Test(form: Form[MatchTaxYearModel] = MatchTaxYearForm.matchTaxYearForm, isEditMode: Boolean = false) {
    val view: HtmlFormat.Appendable = match_to_tax_year(
      matchTaxYearForm = form,
      postAction = ViewSpecTrait.testCall,
      backUrl = ViewSpecTrait.testBackUrl,
      isEditMode = isEditMode
    )(FakeRequest(), applicationMessages, appConfig)
    val document: Document = Jsoup.parse(view.body)
  }

  "The match to tax year view" should {

    "have the correct title" in new Test() {
      document.title() mustBe MessageLookup.Business.MatchTaxYear.heading
    }

    "have the correct heading" in new Test() {
      document.select("h1").text() mustBe MessageLookup.Business.MatchTaxYear.heading
    }

    "have a back link" which {

      "has the correct link" in new Test() {
        document.select("a.back-link").attr("href") mustBe ViewSpecTrait.testBackUrl
      }
    }

    "have the correct content" which {

      "has a correct first paragraph" in new Test() {
        document.select("article p").first().text() mustBe MessageLookup.Business.MatchTaxYear.para1
      }

      "has a correct second paragraph" in new Test() {
        document.select("article p").last().text() mustBe MessageLookup.Business.MatchTaxYear.para2
      }
    }

    "have a form" which {
      lazy val form = new Test().document.select("form")

      "has the correct action" in {
        form.attr("action") mustBe ViewSpecTrait.testCall.url
      }

      "has the correct method" in {
        form.attr("method") mustBe ViewSpecTrait.testCall.method
      }
    }

    "have a radioset" which {
      lazy val radioset = new Test().document.select("fieldset")

      "has the correct legend" in {
        radioset.select("legend").text() mustBe MessageLookup.Business.MatchTaxYear.heading
      }

      "has only two options" in {
        radioset.select("div.multiple-choice").size() mustBe 2
      }

      "has a yes option" which {

        "has the correct label" in {
          radioset.select("""[for="matchToTaxYear-Yes"]""").text() mustBe MessageLookup.Business.MatchTaxYear.yes
        }

        "has the correct value" in {
          radioset.select("#matchToTaxYear-Yes").attr("value") mustBe "Yes"
        }
      }

      "has a no option" which {

        "has the correct label" in {
          radioset.select("""[for="matchToTaxYear-No"]""").text() mustBe MessageLookup.Business.MatchTaxYear.no
        }

        "has the correct value" in {
          radioset.select("#matchToTaxYear-No").attr("value") mustBe "No"
        }
      }
    }

    "has a continue button" which {

      "has the correct text when in edit mode" in new Test(isEditMode = true) {
        document.select("button").text() mustBe MessageLookup.Base.update
      }

      "has the correct text when not in edit mode" in new Test() {
        document.select("button").text() mustBe MessageLookup.Base.continue
      }
    }
  }
}
