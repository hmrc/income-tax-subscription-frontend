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

import assets.MessageLookup.{ContactEmail => messages}
import forms.EmailForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import utils.UnitTestTrait

class ContactEmailViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.business.routes.BusinessAccountingPeriodController.showAccountingPeriod().url
  lazy val page = views.html.contact_email(
    contactEmailForm = EmailForm.emailForm,
    postAction = controllers.routes.ContactEmailController.submitContactEmail(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Contact Email Address view" should {

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

      "has a text input field for the email address" in {
        document.select("input[name=emailAddress]").isEmpty mustBe false
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.routes.ContactEmailController.submitContactEmail().url}'" in {
        document.select("form").attr("action") mustBe controllers.routes.ContactEmailController.submitContactEmail().url
        document.select("form").attr("method") mustBe "POST"
      }
    }
  }
}
