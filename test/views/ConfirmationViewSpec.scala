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
import models.DateModel
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class ConfirmationViewSpec extends UnitTestTrait {

  val subscriptionIdValue = "000-032407"
  val submissionDateValue = DateModel("1", "1", "2016")

  lazy val page = views.html.confirmation(
    subscriptionId = subscriptionIdValue,
    submissionDate = submissionDateValue,
    postAction = controllers.routes.SignOutController.signOut()
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Confirmation view" should {

    s"have the title '${MessageLookup.Confirmation.title}'" in {
      document.title() must be(MessageLookup.Confirmation.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.Confirmation.heading}'" in {
          heading.text() mustBe MessageLookup.Confirmation.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }

      s"has a subscription id value '$subscriptionIdValue'" in {
        document.select("#subscription-id-value").text() mustBe subscriptionIdValue
      }

      s"has in the banner a paragraph of '${MessageLookup.Confirmation.banner_line1_1}'" in {
        document.select("#confirmation-heading p").text() must include(MessageLookup.Confirmation.banner_line1_1)
      }

      s"has in the banner a paragraph of '${MessageLookup.Confirmation.banner_line1_2}'" in {
        document.select("#confirmation-heading p").text() must include(MessageLookup.Confirmation.banner_line1_2)
      }
    }

    "have a 'What happens next' section" which {

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para1)
      }

      s"has the section heading '${MessageLookup.Confirmation.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.Confirmation.whatHappensNext.heading
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para2)
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para3}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para3)
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para4}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para4)
      }

      s"has an Agent Services account link '${MessageLookup.Confirmation.whatHappensNext.linkText}'" in {
        document.select("#whatHappensNext a").text() mustBe MessageLookup.Confirmation.whatHappensNext.linkText
        document.select("#whatHappensNext a").attr("href") mustBe appConfig.btaUrl
      }

    }

    "have a sign out button" in {
      val b = document.getElementById("sign-out-button")
      b.text() mustBe MessageLookup.Confirmation.signOut
    }

  }
}
