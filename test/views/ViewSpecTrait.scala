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
import org.jsoup.select.Elements
import play.twirl.api.Html
import utils.UnitTestTrait


trait ViewSpecTrait extends UnitTestTrait {

  case class Selector(name: String, cssQuery: String)

  trait ElementTest {
    val name: String

    val element: Element

    // n.b. should not be made public since it is not a test nor does it return an ElementTest
    protected def getById(id: String): Element = {
      val ele = element.getElementById(id)
      if (ele == null) fail(s"unable to locate: $id")
      ele
    }

    def getById(name: String, id: String): ElementTest = {
      lazy val n = s"""${this.name}."$name""""
      ElementTest(name, () => element.getElementById(id))
    }

    def selectHead(name: String, cssQuery: String): ElementTest = {
      lazy val n = s"""${this.name}."$name""""
      ElementTest(name, () => element.select(cssQuery).get(0))
    }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(text: String, href: => String) =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.select("a")
        if (link == null) fail(s"Unable to locate any links in $name\n$element\n")
        if (link.size() > 1) fail(s"Multiple links located in $name, please specify an id")
        link.attr("href") mustBe href
        link.text() mustBe text
      }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(id: String, text: String, href: => String) =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.getElementById(id)
        if (link == null) fail(s"Unable to locate $id")
        if (!link.tagName().equals("a")) fail(s"The element with id=$id is not a link")
        link.attr("href") mustBe href
        link.text() mustBe text
      }

    def mustHavePara(paragraph: String) =
      s"$name must have the paragraph (P) '$paragraph'" in {
        element.getElementsByTag("p").text() must include(paragraph)
      }

    def mustHaveSeqParas(paragraphs: String*) = {
      if (paragraphs.isEmpty) fail("Must provide at least 1 paragraph for this test")
      val ps = paragraphs.mkString(" ")
      s"$name must have the paragraphs (P) [${paragraphs.mkString("], [")}]" in {
        element.getElementsByTag("p").text() must include(ps)
      }
    }

    def mustNotHaveParas(paragraphs: String*) =
      for (p <- paragraphs) {
        s"$name must not have the paragraph '$p'" in {
          element.getElementsByTag("p").text() must not include p
        }
      }

    def mustHaveSeqBullets(bullets: String*) = {
      if (bullets.isEmpty) fail("Must provide at least 1 bullet point for this test")
      val bs = bullets.mkString(" ")
      s"$name must have the bulletPoints (LI) [${bullets.mkString("], [")}]" in {
        element.getElementsByTag("LI").text() must include(bs)
      }
    }

    def mustNotHaveBullets(bullets: String*) =
      for (b <- bullets) {
        s"$name must not have the bullet point '$b'" in {
          element.getElementsByTag("LI").text() must not include b
        }
      }

    case class RadioOption(name: String, text: String)

    object RadioOption {
      implicit def conv(radioOption: RadioOption): (String, String) = (radioOption.name, radioOption.text)

      implicit def conv(radioOption: Seq[RadioOption]): Seq[(String, String)] = radioOption.map(x => x: (String, String))

      implicit def conv(radioOption: (String, String)): RadioOption = RadioOption(radioOption._1, radioOption._2)

      implicit class RadioOptionSeqUtil(radioOption: Seq[RadioOption]) {
        def toTuples: Seq[(String, String)] = radioOption: Seq[(String, String)]
      }

    }

    def mustHaveRadioSet(legend: String, radioName: String)(options: RadioOption*) = {
      if (legend.isEmpty) fail("Legend cannot be none empty, this would cause an accessibility issue")
      if (radioName.isEmpty) fail("Must provide the field name which groups all the buttons in this test")
      if (options.isEmpty) fail("Must provide at least 1 radio button for this test")
      if (options.size == 1) fail("It does not make sense to have a radio button fieldset with only a single option")

      s"$name must have a radio fieldset for $legend" which {

        s"has a legend with the text '$legend'" in {
          element.select("fieldset legend").text() mustBe legend
        }

        for ((o, text) <- options.toTuples) {
          s"has a radio option for '$radioName-$o'" in {
            val cashRadio = element.select(s"#$radioName-$o")
            cashRadio.attr("type") mustBe "radio"
            cashRadio.attr("name") mustBe s"$radioName"
            cashRadio.attr("value") mustBe o
            val label = element.getElementsByAttributeValue("for", s"$radioName-$o")
            label.size() mustBe 1
            label.get(0).text() mustBe text
          }
        }

      }

    }

    def mustHaveSubmitButton(text: String) =
      s"$name must have the a submit button (Button) '$text'" in {
        import collection.JavaConversions._
        val submitButtons = element.select("button").filter(_.attr("type").equals("submit"))
        submitButtons.size mustBe 1
        submitButtons.head.text() mustBe text
      }

    def mustHaveContinueButton() = mustHaveSubmitButton(Base.continue)

    def mustHaveUpdateButton() = mustHaveSubmitButton(Base.update)

    def mustHaveCheckbox(id: String, message: String) =
      s"$name must have a checkbox to $message" in {
        val checkbox = getById(id)
        checkbox.attr("type") mustBe "checkbox"
        checkbox.parents().get(0).text() mustBe message
      }

    def mustHaveCheckbox(message: String) =
      s"$name must have a checkbox to $message" in {
        import collection.JavaConversions._
        val checkbox: Elements = new Elements(element.select("input").filter(x => x.attr("type").equals("checkbox")))
        if (checkbox.size() == 0) fail(s"""Unable to locate any checkboxes in "$name""""")
        if (checkbox.size() > 1) fail(s"""Multiple checkboxes located in "$name", please specify an id""")
        checkbox.parents().get(0).text() mustBe message
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
  class TestView(override val name: String, page: => Html, signOutInBanner: Boolean = true) extends ElementTest {

    lazy val document = Jsoup.parse(page.body)
    override lazy val element = document.getElementById("content")

    if (signOutInBanner) {
      s"$name must have a sign out link in the banner" in {
        val signOut = document.getElementById("logOutNavHref")
        if (signOut == null) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
        signOut.text() mustBe Base.signout
        signOut.attr("href") mustBe controllers.routes.SignOutController.signOut().url
      }
    } else {
      s"$name must not have a sign out link in the banner" in {
        val signOut = document.getElementById("logOutNavHref")
        signOut mustBe null
      }
    }

    def mustHaveBackTo(backUrl: String) =
      s"$name must have a back link pointed to '$backUrl'" in {
        val backLink = element.select("#back")
        backLink.isEmpty mustBe false
        backLink.attr("href") mustBe backUrl
      }

    def mustHaveTitle(title: String) =
      s"$name must have the title '$title'" in {
        document.title() mustBe title
      }

    def mustHaveH1(heading: String) =
      s"$name must have the heading (H1) '$heading'" in {
        val h1 = document.getElementsByTag("H1")
        h1.size() mustBe 1
        h1.text() mustBe heading
      }

    // this method returns either the first form in the document or one specified by id
    // @param method expected method used by the form, e.g. "GET", "POST"
    // @oaram action expected action used by the form, i.e. the destination url
    // n.b. action must be call-by-name otherwise if the parameter is generated from a call
    // it could be evaluated with the wrong context root
    def getForm(name: String, id: Option[String] = None)(method: String, action: => String): ElementTest = {
      val selector =
        id match {
          case Some(i) => s"#$i"
          case _ => "form"
        }

      // this test is put in here because it doesn't make sense for it to be called on anything
      // other than a form
      s"$name has a $method action to '$action'" in {
        document.select(selector).attr("method") mustBe method.toUpperCase
        document.select(selector).attr("action") mustBe action
      }
      selectHead(name, selector)
    }

  }

  object TestView {
    // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
    def apply(name: String, page: => Html, signOutInBanner: Boolean = true): TestView = new TestView(name, page, signOutInBanner)
  }

}
