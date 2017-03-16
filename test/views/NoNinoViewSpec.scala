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

import assets.MessageLookup.Base
import assets.MessageLookup.NoNino._
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class NoNinoViewSpec extends UnitTestTrait {

  lazy val page = views.html.no_nino(postAction = controllers.routes.NoNinoController.submitNoNino())(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The No Nino view" should {

    s"have the title '$title'" in {
      document.title() must be(title)
    }

    s"have the heading (H1) '$heading'" in {
      document.getElementsByTag("H1").text() must be(heading)
    }

    s"have the paragraph (P) '$line1'" in {
      document.getElementsByTag("P").text() must include(line1)
    }

    "have a sign-out button" in {
      document.select("button").attr("type") mustBe "submit"
      document.select("button").text() mustBe Base.signout
    }
  }
}
