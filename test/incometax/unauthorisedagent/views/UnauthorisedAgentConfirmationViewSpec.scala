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

package incometax.unauthorisedagent.views

import assets.MessageLookup
import core.controllers.SignOutController
import core.models.DateModel
import core.views.ViewSpecTrait
import incometax.subscription.models.{Both, Business, IncomeSourceType}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html

class UnauthorisedAgentConfirmationViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val duration: Int = 0
  val action = ViewSpecTrait.testCall
  val incomeSource = Business
  val request = ViewSpecTrait.viewTestRequest

  def page(incomeSource: IncomeSourceType): Html = incometax.unauthorisedagent.views.html.unauthorised_agent_confirmation(
    journeyDuration = duration,
    incomeSource = incomeSource
  )(request, applicationMessages, appConfig)

  def document = Jsoup.parse(page(incomeSource).body)

  "The Confirmation view for both income source" should {

    s"have the title '${MessageLookup.Confirmation.Unauthorised.title}'" in {
      document.title() must be(MessageLookup.Confirmation.Unauthorised.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.Confirmation.Unauthorised.heading}'" in {
          heading.text() mustBe MessageLookup.Confirmation.Unauthorised.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }
    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.heading
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.Unauthorised.whatHappensNext.para1)
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.Unauthorised.whatHappensNext.para2)
      }

      s"has a list of actions for the client to perform" in {
        val list = document.select("#actionList li")
        list.get(0).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.list1
        list.get(1).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.list2
        list.get(2).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.list3
        list.get(3).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.list4
        list.get(4).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.list5
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.para3}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.Unauthorised.whatHappensNext.para3)
      }

      s"has the correct bullet points" in {
        val bullets = document.select("#bullets li")
        bullets.get(0).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.bul1
        bullets.get(0).select("a").attr("href") mustBe appConfig.softwareUrl

        bullets.get(1).text mustBe MessageLookup.Confirmation.Unauthorised.whatHappensNext.bul2
        bullets.get(1).select("a").attr("href") mustBe appConfig.btaUrl
      }

      s"does not have a paragraph stating HMRC process '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.para4}'" in {
        document.select("#whatHappensNext p").text() must not include MessageLookup.Confirmation.whatHappensNext.para4
      }

    }

    "have a sign out button" in {
      val actionSignOut = document.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut(request.path).url
    }

  }

  "The Confirmation view for both income source" should {
    s"have a paragraph stating HMRC process '${MessageLookup.Confirmation.Unauthorised.whatHappensNext.para4}'" in {
      Jsoup.parse(page(Both).body).select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para4)
    }
  }
}
