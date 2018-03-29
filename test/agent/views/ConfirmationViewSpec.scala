/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.views

import agent.assets.MessageLookup
import core.models.DateModel
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import core.utils.UnitTestTrait
import core.views.ViewSpecTrait

class ConfirmationViewSpec extends UnitTestTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val action = ViewSpecTrait.testCall

  lazy val page = agent.views.html.confirmation(
    submissionDate = submissionDateValue,
    postAction = agent.controllers.routes.AddAnotherClientController.addAnother(),
    signOutAction = action
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

    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.Confirmation.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.Confirmation.whatHappensNext.heading
      }

      s"has a paragraph stating complete steps '${MessageLookup.Confirmation.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para1)
      }

      s"has a paragraph stating software package '${MessageLookup.Confirmation.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para2)
      }

      s"has an initial numeric point '${MessageLookup.Confirmation.whatHappensNext.number1}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.Confirmation.whatHappensNext.number1)
      }

      s"has a 2nd numeric point '${MessageLookup.Confirmation.whatHappensNext.number2}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.Confirmation.whatHappensNext.number2)
      }

      s"has a 3rd numeric point '${MessageLookup.Confirmation.whatHappensNext.number3}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.Confirmation.whatHappensNext.number3)
      }

      s"has a 4th numeric point '${MessageLookup.Confirmation.whatHappensNext.number4}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.Confirmation.whatHappensNext.number4)
      }

      s"has a 5th numeric point '${MessageLookup.Confirmation.whatHappensNext.number5}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.Confirmation.whatHappensNext.number5)
      }

      s"has a paragraph stating Income Tax Estimate '${MessageLookup.Confirmation.whatHappensNext.para3}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para3)
      }

      s"has the correct bullet points" in {
        val bullets = document.select("#bullets li")
        bullets.get(0).text mustBe MessageLookup.Confirmation.whatHappensNext.bullet1

        bullets.get(1).text mustBe MessageLookup.Confirmation.whatHappensNext.bullet2
        bullets.get(1).select("a").attr("href") mustBe appConfig.btaUrl
      }


    }

    "have a add another client button" in {
      val b = document.getElementById("add-another-button")
      b.text() mustBe MessageLookup.Base.addAnother
    }

  }
}
