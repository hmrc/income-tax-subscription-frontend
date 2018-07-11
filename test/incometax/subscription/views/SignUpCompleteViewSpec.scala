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

package incometax.subscription.views

import assets.MessageLookup
import core.controllers.SignOutController
import core.models.DateModel
import core.utils.TestModels.{testSummaryData, testWorkForYourself_no}
import core.views.ViewSpecTrait
import incometax.subscription.models.{Both, Business, IncomeSourceType}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html

class SignUpCompleteViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val duration: Int = 0
  val action = ViewSpecTrait.testCall
  val incomeSource = Business
  val request = ViewSpecTrait.viewTestRequest

  def page(incomeSource: IncomeSourceType): Html = incometax.subscription.views.html.sign_up_complete(
    journeyDuration = duration,
    summary = incomeSource match {
      case Both => testSummaryData
      case _ => testSummaryData.copy(workForYourself = Some(testWorkForYourself_no))
    }
  )(request, applicationMessages, appConfig)

  def document = Jsoup.parse(page(incomeSource).body)

  "The Confirmation view for Business income source" should {

    s"have the title '${MessageLookup.SignUpComplete.title}'" in {
      document.title() must be(MessageLookup.SignUpComplete.title)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.SignUpComplete.heading}'" in {
          heading.text() mustBe MessageLookup.SignUpComplete.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }
    }

    "have a 'What happens next' section" which {

      s"has the section heading '${MessageLookup.SignUpComplete.whatHappensNext.heading}'" in {
        document.select("#whatHappensNext h2").text() mustBe MessageLookup.SignUpComplete.whatHappensNext.heading
      }

      s"has a numeric list of actions for the Individual to perform" in {
        val list = document.select("#actionList li")
        list.get(0).text mustBe MessageLookup.SignUpComplete.whatHappensNext.number1
        list.get(0).select("a").attr("href") mustBe appConfig.softwareUrl
        list.get(1).text mustBe MessageLookup.SignUpComplete.whatHappensNext.number2
        list.get(2).text mustBe MessageLookup.SignUpComplete.whatHappensNext.number3
        list.get(3).text mustBe MessageLookup.SignUpComplete.whatHappensNext.number4
        list.get(4).text mustBe MessageLookup.SignUpComplete.whatHappensNext.number5
      }

      s"has a paragraph referring to Income Tax Estimate '${MessageLookup.SignUpComplete.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.SignUpComplete.whatHappensNext.para1)
      }

      s"has a bullet point '${MessageLookup.SignUpComplete.whatHappensNext.bullet1}'" in {
        val softwareBullet = document.select("#whatHappensNext ul li").first()
        softwareBullet.text() mustBe MessageLookup.SignUpComplete.whatHappensNext.bullet1
      }

      s"has a bullet point to BTA '${MessageLookup.SignUpComplete.whatHappensNext.bullet2}'" in {
        val bul2 = document.select("#whatHappensNext ul li").get(1)
        bul2.text() mustBe MessageLookup.SignUpComplete.whatHappensNext.bullet2
        bul2.select("a").attr("href") mustBe appConfig.btaUrl
      }

      s"does not have a paragraph stating information to appear '${MessageLookup.SignUpComplete.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must not include MessageLookup.SignUpComplete.whatHappensNext.para2
      }

    }

    "have a sign out button" in {
      val actionSignOut = document.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut(request.path).url
    }

  }

  "The Sign Up view for both income source" should {
    s"have a paragraph stating HMRC process '${MessageLookup.SignUpComplete.whatHappensNext.para2}'" in {
      Jsoup.parse(page(Both).body).select("#whatHappensNext p").text() must include(MessageLookup.SignUpComplete.whatHappensNext.para2)
    }
  }
}
