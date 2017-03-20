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

import assets.MessageLookup.{Base => common, FrontPage => messages}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class FrontPageViewSpec extends UnitTestTrait {

  lazy val page = views.html.frontpage(
    getAction = controllers.routes.HomeController.index()
  )(FakeRequest(), applicationMessages, appConfig)

  lazy val document = Jsoup.parse(page.body)

  "The 'Front/Start Page view" should {

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() mustBe messages.heading
    }

    s"has a paragraph (P) for '${messages.line_1}'" in {
      document.getElementsByTag("p").text() must include (messages.line_1)
    }

    s"has a paragraph (P) for '${messages.line_2}'" in {
      document.getElementsByTag("p").text() must include (messages.line_2)
    }

    s"has a bullet for '${messages.bullet_1}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_1)
    }

    s"has a bullet for '${messages.bullet_2}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_2)
    }

    s"has a paragraph (P) for '${messages.line_3}'" in {
      document.getElementsByTag("p").text() must include (messages.line_3)
    }

    s"has a paragraph (P) for '${messages.line_4}'" in {
      document.getElementsByTag("p").text() must include (messages.line_4)
    }

    s"has a paragraph (P) for '${messages.line_5}'" in {
      document.getElementsByTag("p").text() must include (messages.line_5)
    }

    s"has a paragraph (P) for '${messages.line_6}'" in {
      document.getElementsByTag("p").text() must include (messages.line_6)
    }

    s"has a bullet for '${messages.bullet_3}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_3)
    }

    s"has a bullet for '${messages.bullet_4}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_5)
    }

    s"has a bullet for '${messages.bullet_5}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_5)
    }

    s"has a bullet for '${messages.bullet_6}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_6)
    }

    s"has a bullet for '${messages.bullet_7}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_7)
    }

    s"has a paragraph (P) for '${messages.line_7}'" in {
      document.getElementsByTag("p").text() must include (messages.line_7)
    }

    "have a form" which {

      s"has a GET action to '${controllers.routes.HomeController.index().url}'" in {
        document.select("form").attr("method") mustBe "GET"
        document.select("form").attr("action") mustBe controllers.routes.HomeController.index().url
      }

      "has a 'Sign Up' button" in {
        document.select("button").attr("type") mustBe "submit"
        document.select("button").text() mustBe common.signUp
      }

    }

    s"has a Heading 2 (H2) for '${messages.h2}'" in {
      document.getElementsByTag("h2").text() must include (messages.h2)
    }

    s"has a paragraph (P) for '${messages.line_8}'" in {
      document.getElementsByTag("p").text() must include (messages.line_8)
    }

    s"has a bullet for '${messages.bullet_8}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_8)
    }

    s"has a bullet for '${messages.bullet_9}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_9)
    }

    s"has a paragraph (P) for '${messages.line_9}'" in {
      document.getElementsByTag("p").text() must include (messages.line_9)
    }

    s"has a bullet for '${messages.bullet_10}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_10)
    }

    s"has a bullet for '${messages.bullet_11}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_11)
    }

    s"has a paragraph (P) for '${messages.line_10}'" in {
      document.getElementsByTag("p").text() must include (messages.line_10)
    }

    s"has a bullet for '${messages.bullet_12}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_12)
    }

    s"has a bullet for '${messages.bullet_13}'" in {
      document.getElementsByTag("li").text() must include (messages.bullet_13)
    }
  }
}
