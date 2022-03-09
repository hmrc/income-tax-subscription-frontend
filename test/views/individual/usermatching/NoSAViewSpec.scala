/*
 * Copyright 2022 HM Revenue & Customs
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

package views.individual.usermatching

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.usermatching.NoSA

class NoSAViewSpec extends ViewSpec {

  val noSA: NoSA = app.injector.instanceOf[NoSA]
  val page: Html = noSA()(request, implicitly)
  val document: Document = Jsoup.parse(page.body)

  object NoSAMessages {
    val heading: String = "You need to register for Self Assessment"
    val linkText = "register for Self Assessment."
    val info = s"Before you can sign up to use software to report your Income Tax, you need to $linkText"
  }

  "NoSA" must {

    "have the correct template" in new TemplateViewTest(
      view = page,
      title = NoSAMessages.heading,
    )

    "have a heading" in {
      document.mainContent.getH1Element.text mustBe NoSAMessages.heading
    }

    "have a paragraph of info" which {
      "has information for the user" in {
        document.mainContent.getNthParagraph(1).text mustBe NoSAMessages.info
      }
      "has a link" in {
        val link: Element = document.mainContent.getNthParagraph(1).getLink("sa-signup")
        link.text mustBe NoSAMessages.linkText
        link.attr("href") mustBe appConfig.signUpToSaLink
      }
    }

  }

}
