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

package views.helpers

import assets.MessageLookup
import controllers.SignOutController
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utilities.UnitTestTrait
import views.html.helpers.signOutLink

class SignOutLinkHelperSpec extends UnitTestTrait {

  def view(alternateText: Option[String])(request: Request[_]): HtmlFormat.Appendable = signOutLink(alternateText)(request, implicitly)

  def html(alternateText: Option[String])(request: Request[_]): Document = Jsoup.parse(view(alternateText)(request).body)

  "The sign out button helper" should {

    val testRequest = FakeRequest("GET", "/test-path")

    "allow creation of a button with default text" which {
      val default = html(None)(testRequest)

      val signOutButton = default.getElementById("sign-out")

      "is of tag button" in {
        signOutButton.isEmpty mustBe false
      }

      s"has the text '${MessageLookup.Base.signOut}'" in {
        signOutButton.text() mustBe MessageLookup.Base.signOut
      }

      s"point to sign out with the path from the request" in {
        signOutButton.attr("href") mustBe SignOutController.signOut.url
      }
    }

    "allow creation of a button with alternate text" which {
      val alternateText = "alternate text"
      val default = html(alternateText)(testRequest)

      val signOutButton = default.getElementById("sign-out")

      "is of tag button" in {
        signOutButton.isEmpty mustBe false
      }

      s"has the text '$alternateText'" in {
        signOutButton.text() mustBe alternateText
      }

      s"point to sign out with the path from the request" in {
        signOutButton.attr("href") mustBe SignOutController.signOut.url
      }
    }
  }
}
