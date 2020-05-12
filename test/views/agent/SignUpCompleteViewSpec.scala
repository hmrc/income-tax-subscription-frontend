/*
 * Copyright 2020 HM Revenue & Customs
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

package views.agent

import agent.assets.MessageLookup
import utilities.TestModels.testAgentSummaryData
import models.DateModel
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utilities.UnitTestTrait
import views.ViewSpecTrait

class SignUpCompleteViewSpec extends UnitTestTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val action = ViewSpecTrait.testCall

  lazy val page = views.html.agent.sign_up_complete(
    summary = testAgentSummaryData,
    postAction = controllers.agent.routes.AddAnotherClientController.addAnother(),
    signOutAction = action
  )(FakeRequest(), implicitly, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Sign Up Complete view" should {

    s"have the title '${MessageLookup.SignUpComplete.title}'" in {
      document.title() must be(MessageLookup.SignUpComplete.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("govuk-box-highlight") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.SignUpComplete.heading}'" in {
          heading.text() mustBe MessageLookup.SignUpComplete.heading
        }

        "has the class 'heading-large'" in {
          heading.hasClass("heading-large") mustBe true
        }
      }

    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.SignUpComplete.whatNext.heading}'" in {
        document.select("#whatNext h2").text() mustBe MessageLookup.SignUpComplete.whatNext.heading
      }

      s"has a paragraph stating complete steps '${MessageLookup.SignUpComplete.whatNext.para1}'" in {
        document.select("#whatNext p").get(0).text() mustBe MessageLookup.SignUpComplete.whatNext.para1
      }

      s"has an initial numeric point '${MessageLookup.SignUpComplete.whatNext.number1}'" in {
        document.select("#whatNext li").get(0).text() mustBe MessageLookup.SignUpComplete.whatNext.number1
        document.select("#whatNext li").get(0).select("a").attr("href") mustBe appConfig.softwareUrl
      }

      s"has a 2nd numeric point '${MessageLookup.SignUpComplete.whatNext.number2}'" in {
        document.select("#whatNext li").get(1).text() mustBe MessageLookup.SignUpComplete.whatNext.number2
      }

      s"has a 3rd numeric point '${MessageLookup.SignUpComplete.whatNext.number3}'" in {
        document.select("#whatNext li").get(2).text() mustBe MessageLookup.SignUpComplete.whatNext.number3
      }

      s"has a 4th numeric point '${MessageLookup.SignUpComplete.whatNext.number4}'" in {
        document.select("#whatNext li").get(3).text() mustBe MessageLookup.SignUpComplete.whatNext.number4
      }

      s"has a 5th numeric point '${MessageLookup.SignUpComplete.whatNext.number5}'" in {
        document.select("#whatNext li").get(4).text() mustBe MessageLookup.SignUpComplete.whatNext.number5
      }

      s"has a paragraph stating Income Tax Estimate '${MessageLookup.SignUpComplete.whatNext.para2}'" in {
        document.select("#whatNext p").get(1).text() mustBe MessageLookup.SignUpComplete.whatNext.para2
      }

    }

    "have a add another client button" in {
      val b = document.getElementById("add-another-button")
      b.text() mustBe MessageLookup.Base.addAnother
    }

  }
}
