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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.twirl.api.Html
import utils.UnitTestTrait


trait ViewSpecTrait extends UnitTestTrait {

  case class Selector(name: String, cssQuery: String)

  trait ElementTest {
    val name: String

    val element: Element

    def mustHaveTheFollowingParagaphs(paragraph: String) =
      s"$name must have the paragraph (P) '$paragraph'" in {
        element.getElementsByTag("p").text() must include(paragraph)
      }

    def mustHaveSubmitButton(text: String) =
      s"$name must have the a submit button (Button) '$text'" in {
        import collection.JavaConversions._
        val submitButtons = element.select("button").filter(_.attr("type").equals("submit"))
        submitButtons.size mustBe 1
        submitButtons.head.text() mustBe text
      }

    def mustHaveContinueButton = mustHaveSubmitButton(Base.continue)

    def mustHaveUpdateButton = mustHaveSubmitButton(Base.update)

    def selectHead(name: String, cssQuery: String): ElementTest = {
      lazy val n = s"""${this.name}."$name""""
      ElementTest(name, () => element.select(cssQuery).get(0))
    }

  }

  object ElementTest {

    // n.b. element must be null-ary function to prevent evaluation at instantiation
    def apply(name: String, element: () => Element): ElementTest = {
      val n = name
      val ele = element
      element match {
        case null => throw new IllegalArgumentException("creation of name failed: element is null")
        case _ => new ElementTest {
          override lazy val name: String = n
          override lazy val element: Element = ele()
        }
      }
    }

  }

  // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
  class TestView(override val name: String, page: => Html) extends ElementTest {

    lazy val document = Jsoup.parse(page.body)
    override lazy val element = document

    def mustHaveTheTitle(title: String) =
      s"$name must have the title '$title'" in {
        document.title() mustBe title
      }

    def mustHaveTheHeading(heading: String) =
      s"$name must have the heading (H1) '$heading'" in {
        val h1 = document.getElementsByTag("H1")
        h1.size() mustBe 1
        h1.text() mustBe heading
      }

    // this method returns either the first form in the document or one specified by id
    // n.b. action must be call-by-name otherwise if the parameter is generated from a call
    // it could be evaluated with the wrong context root
    def getForm(name: String, id: Option[String] = None)(method: String, action: => String): ElementTest = {
      val selector =
        id match {
          case Some(i) => s"#$i"
          case _ => "form"
        }

      s"$name has a $method action to '$action'" in {
        document.select(selector).attr("method") mustBe method.toUpperCase
        document.select(selector).attr("action") mustBe action
      }
      selectHead(name, selector)
    }

  }

  object TestView {
    // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
    def apply(name: String, page: => Html): TestView = new TestView(name, page)
  }


}
