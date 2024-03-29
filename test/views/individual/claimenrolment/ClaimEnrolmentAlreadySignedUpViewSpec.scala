/*
 * Copyright 2023 HM Revenue & Customs
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

package views.individual.claimenrolment

import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.twirl.api.Html
import views.ViewSpecTrait
import views.html.individual.claimenrolment.ClaimEnrolmentAlreadySignedUp

class ClaimEnrolmentAlreadySignedUpViewSpec extends ViewSpecTrait {

  val claimEnrolmentAlreadySignedUp: ClaimEnrolmentAlreadySignedUp = app.injector.instanceOf[ClaimEnrolmentAlreadySignedUp]

  val request = ViewSpecTrait.viewTestRequest

  val page: Html = claimEnrolmentAlreadySignedUp()(request, implicitly, appConfig)
  val document: Document = Jsoup.parse(page.body)

  "The Claim Enrolment Already Signed Up view" should {

    s"have the title '${MessageLookup.ClaimEnrolmentAlreadySignedUp.title}'" in {
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      document.title() must be(MessageLookup.ClaimEnrolmentAlreadySignedUp.title + serviceNameGovUk)
    }

    s"have a heading (H1)" which {
      lazy val heading = document.select("H1")
      s"has the text '${MessageLookup.ClaimEnrolmentAlreadySignedUp.title}'" in {
        heading.text() must startWith(MessageLookup.ClaimEnrolmentAlreadySignedUp.title)
      }
    }

    "have a content section" which {

      s"has a link to '${MessageLookup.ClaimEnrolmentAlreadySignedUp.link2}'" in {
        val link2 = document.select("a[id=retrieveSaAccountDetails]")
        link2.text mustBe MessageLookup.ClaimEnrolmentAlreadySignedUp.link2

      }

      s"has some content '${MessageLookup.ClaimEnrolmentAlreadySignedUp.content}'" in {
        val content = document.select("p[id=claimAlreadySignedInBody]")
        content.text() must startWith(MessageLookup.ClaimEnrolmentAlreadySignedUp.content)
      }
    }
    "has the correct link in page for bta to sa utr" in {
      val btaLink: Elements = document.select("a[id=checkSaAccount]")
      btaLink.text mustBe MessageLookup.ClaimEnrolmentAlreadySignedUp.link1
      btaLink.attr("href") mustBe appConfig.haveSaUtr
    }
    "has the correct link in page for bta wrong credentials " in {
      val btaLink: Elements = document.select("a[id=retrieveSaAccountDetails]")
      btaLink.text mustBe MessageLookup.ClaimEnrolmentAlreadySignedUp.link2
      btaLink.attr("href") mustBe appConfig.wrongCredentials
    }
  }
}