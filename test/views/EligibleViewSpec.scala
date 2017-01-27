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

class EligibleViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.ContactEmailController.showContactEmail().url
  lazy val page = views.html.eligible(
    postAction = controllers.routes.EligibleController.submitEligible(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Eligible view" should {

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document.select("#back")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
    }

    s"have the title '${MessageLookup.Eligible.title}'" in {
      document.title() must be(MessageLookup.Eligible.title)
    }

    s"have the heading (H1) '${MessageLookup.Eligible.heading}'" in {
      document.getElementsByTag("H1").text() must be(MessageLookup.Eligible.heading)
    }

    s"have the paragraph 1 (P) '${MessageLookup.Eligible.line_1}'" in {
      document.getElementsByTag("P").text() must include(MessageLookup.Eligible.line_1)
    }

    s"have the paragraph 2 (P) '${MessageLookup.Eligible.line_2}'" in {
      document.getElementsByTag("P").text() must include(MessageLookup.Eligible.line_2)
    }

    "have a form" which {

      s"has a post action to '${controllers.routes.EligibleController.submitEligible().url}'" in {
        document.select("form").attr("method") mustBe "POST"
        document.select("form").attr("action") mustBe controllers.routes.EligibleController.submitEligible().url
      }

      "has a continue button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe MessageLookup.Base.continue
      }

    }
  }
}
