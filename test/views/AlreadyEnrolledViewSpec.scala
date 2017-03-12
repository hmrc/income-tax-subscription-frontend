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
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Call
import play.api.test.FakeRequest
import utils.UnitTestTrait

class AlreadyEnrolledViewSpec extends UnitTestTrait {

  lazy val testPostRoute = "testPostUrl"
  lazy val page = views.html.enrolled.already_enrolled(Call("POST",testPostRoute))(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Already Enrolled view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      document.title() must be(MessageLookup.AlreadyEnrolled.title)
    }

    s"have the heading (H1) '${MessageLookup.AlreadyEnrolled.heading}'" in {
      document.getElementsByTag("H1").text() must be(MessageLookup.AlreadyEnrolled.heading)
    }

    s"have the paragraph (p) '${MessageLookup.AlreadyEnrolled.para1}'" in {
      document.getElementsByTag("p").text() must include(MessageLookup.AlreadyEnrolled.para1)
    }

    "has a form" which {

      "has a 'Sign Out' button" in {
        document.select("#sign-out-button").isEmpty mustBe false
      }

      s"has a post action to '$testPostRoute'" in {
        document.select("form").attr("action") mustBe testPostRoute
        document.select("form").attr("method") mustBe "POST"
      }
    }
  }
}
