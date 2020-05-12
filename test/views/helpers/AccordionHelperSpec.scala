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

package views.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html

class AccordionHelperSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  private def accordionHelper(label: String,content: Html)
  = views.html.helpers.accordionHelper(label,content)

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  "accordionHelper" should {
    "populate the relevant content in the correct positions" in {
      val testLabel = "my test label text"
      val content = Html("my test content text")

      val doc = accordionHelper( testLabel,content).doc

      doc.getElementsByTag("summary").text() should include(testLabel)
      doc.getElementsByTag("details").text() should include(content.toString())

    }

  }
}
