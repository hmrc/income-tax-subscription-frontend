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
      document.title() must be(messages.title)
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.getElementsByTag("H1").text() must be(messages.heading)
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
  }
}
