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

import assets.MessageLookup
import forms.TermForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class TermsViewSpec extends UnitTestTrait {

  lazy val page = views.html.terms(
    termsForm = TermForm.termForm,
    postAction = controllers.routes.TermsController.submitTerms()
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Terms view" should {

    s"have the title '${MessageLookup.Terms.title}'" in {
      document.title() must be(MessageLookup.Terms.title)
    }

    s"have the heading (H1) '${MessageLookup.Terms.heading}'" in {
      document.getElementsByTag("H1").text() must be(MessageLookup.Terms.heading)
    }

    "have a form" which {

      s"has a post action to '${controllers.routes.TermsController.submitTerms().url}'" in {
        document.select("form").attr("method") mustBe "POST"
        document.select("form").attr("action") mustBe controllers.routes.TermsController.submitTerms().url
      }

      "has a checkbox to agree to the terms and conditions" in {
        document.select("input").attr("type") mustBe "checkbox"
      }

      "has a continue button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe MessageLookup.Base.continue
      }

    }
  }
}
