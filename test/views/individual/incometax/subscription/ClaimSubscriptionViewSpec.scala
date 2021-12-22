/*
 * Copyright 2021 HM Revenue & Customs
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

package views.individual.incometax.subscription

import assets.MessageLookup
import controllers.SignOutController
import models.DateModel
import org.jsoup.Jsoup
import views.ViewSpecTrait
import views.html.individual.incometax.subscription.enrolled.ClaimSubscription

class ClaimSubscriptionViewSpec extends ViewSpecTrait {
  private val request = ViewSpecTrait.viewTestRequest

  private val claimSubscriptionView = app.injector.instanceOf[ClaimSubscription]
  private lazy val page = claimSubscriptionView()(request, implicitly, appConfig)
  private lazy val document = Jsoup.parse(page.body)

  "The Confirmation view" should {

    s"have the title '${MessageLookup.Confirmation.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title() must be(MessageLookup.ClaimSubscription.title + serviceNameGovUk)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        document.select("#confirmation-heading").hasClass("transaction-banner--complete") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.Confirmation.heading}'" in {
          heading.text() mustBe MessageLookup.ClaimSubscription.heading
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

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para1}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para1)
      }

      s"has a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para2}'" in {
        document.select("#whatHappensNext p").text() must include(MessageLookup.Confirmation.whatHappensNext.para2)
      }

      s"has a bullet point '${MessageLookup.Confirmation.whatHappensNext.bul1}'" in {
        val softwareBullet = document.select("#whatHappensNext li").first()
        softwareBullet.text() mustBe MessageLookup.Confirmation.whatHappensNext.bul1
        softwareBullet select "a" attr "href" mustBe appConfig.softwareUrl
      }

      s"has a bullet point to BTAk '${MessageLookup.Confirmation.whatHappensNext.bul2}'" in {
        val btaBullet = document.select("#whatHappensNext li").get(1)
        btaBullet.text() mustBe MessageLookup.Confirmation.whatHappensNext.bul2
        btaBullet select "a" attr "href" mustBe appConfig.btaUrl
      }

      s"does not have a paragraph stating HMRC process '${MessageLookup.Confirmation.whatHappensNext.para4}'" in {
        document.select("#whatHappensNext p").text() must not include MessageLookup.Confirmation.whatHappensNext.para4
      }
    }

    "have a sign out button" in {
      val actionSignOut = document.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut.url
    }

  }
}
