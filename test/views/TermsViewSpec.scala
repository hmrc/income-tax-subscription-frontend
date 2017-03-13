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

import assets.MessageLookup.{Base, Terms => messages}
import forms.TermForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class TermsViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url
  lazy val page = views.html.terms(
    termsForm = TermForm.termForm,
    postAction = controllers.routes.TermsController.submitTerms(),
    backUrl = backUrl,
    isEditMode = false
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Terms view" should {

    s"have the title '${messages.title}'" in {
      document.title() must be(messages.title)
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() must be(messages.heading)
    }

    s"have the line_1 (P) '${messages.line_1}'" in {
      document.getElementsByTag("p").text() must include(messages.line_1)
    }

    s"have the line_2 (P) '${messages.line_2}'" in {
      document.getElementsByTag("p").text() must include(messages.line_2)
    }

    s"have the li_1 (li) '${messages.li_1}'" in {
      document.getElementsByTag("li").text() must include(messages.li_1)
    }

    s"have the li_2 (li) '${messages.li_2}'" in {
      document.getElementsByTag("li").text() must include(messages.li_2)
    }

    s"have the li_3 (li) '${messages.li_3}'" in {
      document.getElementsByTag("li").text() must include(messages.li_3)
    }

    "have a form" which {

      s"has a post action to '${controllers.routes.TermsController.submitTerms().url}'" in {
        document.select("form").attr("method") mustBe "POST"
        document.select("form").attr("action") mustBe controllers.routes.TermsController.submitTerms().url
      }

      "has a checkbox to agree to the terms and conditions" in {
        document.select("input").attr("type") mustBe "checkbox"
        document.select("input").parents().get(0).text() mustBe messages.checkbox
      }

      "has a continue button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe Base.continue
      }

    }
  }

  "When in edit mode, the terms view" should {
    lazy val editPage = views.html.terms(
      termsForm = TermForm.termForm,
      postAction = controllers.routes.TermsController.submitTerms(),
      backUrl = backUrl,
      isEditMode = true
    )(FakeRequest(), applicationMessages, appConfig)
    lazy val editDocument = Jsoup.parse(editPage.body)

    "have an 'Update' button" in {
      editDocument.select("button").attr("type") mustBe "submit"
      editDocument.select("button").text() mustBe Base.update
    }
  }
}
