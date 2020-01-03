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

package agent.views

import agent.assets.MessageLookup
import core.utils.UnitTestTrait
import core.views.ViewSpecTrait
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class UnauthorisedAgentConfirmationViewSpec extends UnitTestTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = agent.views.html.unauthorised_agent_confirmation(
    postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Unauthorised Agent Confirmation view" should {

    s"have the title '${MessageLookup.UnauthorisedAgentConfirmation.title}'" in {
      document.title() must be(MessageLookup.UnauthorisedAgentConfirmation.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (h1)" which {

        lazy val heading = document.select("#confirmation-heading h1")

        s"has the text '${MessageLookup.UnauthorisedAgentConfirmation.heading}'" in {
          heading.text() mustBe MessageLookup.UnauthorisedAgentConfirmation.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }

      s"has a URL (span)" which {

        lazy val url = document.select("#confirmation-heading span")

        s"has the text '${MessageLookup.UnauthorisedAgentConfirmation.url}'" in {
          url.text() mustBe MessageLookup.UnauthorisedAgentConfirmation.url
        }
      }

    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.heading
      }

      s"has an opening paragraph '${MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.para1)
      }

      s"has a bullet point '${MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.bullet1}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.bullet1)
      }

      s"has a further bullet point '${MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.bullet2}'" in {
        document.select("#whatHappensNext li").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.bullet2)
      }

      s"has a closing paragraph '${MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whatHappensNext.para2)
      }
    }

    "have a 'When you're authorised' section" which {

      s"has the section heading '${MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.heading}'" in {
        document.select("#whenAuthorised h2").text() mustBe MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.heading
      }

      s"has an opening paragraph '${MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.para1}'" in {
        document.select("#whenAuthorised p").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.para1)
      }

      s"has an initial numeric point '${MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number1}'" in {
        document.select("#whenAuthorised li").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number1)
      }

      s"has a 2nd numeric point '${MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number2}'" in {
        document.select("#whenAuthorised li").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number2)
      }

      s"has a final numeric point '${MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number3}'" in {
        document.select("#whenAuthorised li").text() must include(MessageLookup.UnauthorisedAgentConfirmation.whenAuthorised.number3)
      }

    }

    "have a add another client button" in {
      val b = document.getElementById("add-another-button")
      b.text() mustBe MessageLookup.Base.addAnother

    }

  }
}
