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
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest

class ContactEmailViewSpec extends PlaySpec with OneAppPerTest {

  lazy val page = views.html.contact_email(
    contactEmailForm = EmailForm.emailForm,
    postAction = controllers.routes.ContactEmailController.submitContactEmail()
  )(FakeRequest(), applicationMessages)
  lazy val document = Jsoup.parse(page.body)

  "The Contact Email Address view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
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
