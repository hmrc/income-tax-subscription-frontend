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

import assets.MessageLookup.{Base => common}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.mvc.Call
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
      ElementTest(n, () => {
        val selector = element.select(cssQuery)
        if (selector.isEmpty) fail(s"Unable to locate $cssQuery in\n$element")
        selector.get(0)
      })
    }

    def mustHaveH2(text: String): Unit =
      s"$name have a Heading 2 (H2) for '$text'" in {
        element.getElementsByTag("h2").text() must include(text)
      }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.select("a")
        if (link == null) fail(s"Unable to locate any links in $name\n$element\n")
        if (link.size() > 1) fail(s"Multiple links located in $name, please specify an id")
        link.attr("href") mustBe href
        link.text() mustBe text
      }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(id: String, text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.getElementById(id)
        if (link == null) fail(s"Unable to locate $id")
        if (!link.tagName().equals("a")) fail(s"The element with id=$id is not a link")
        link.attr("href") mustBe href
        link.text() mustBe text
      }

    def mustHavePara(paragraph: String): Unit =
      s"$name must have the paragraph (P) '$paragraph'" in {
        element.getElementsByTag("p").text() must include(paragraph)
      }

    def mustHaveParaSeq(paragraphs: String*): Unit = {
      if (paragraphs.isEmpty) fail("Must provide at least 1 paragraph for this test")
      val ps = paragraphs.mkString(" ")
      s"$name must have the paragraphs (P) [${paragraphs.mkString("], [")}]" in {
        element.getElementsByTag("p").text() must include(ps)
      }
    }

    def mustNotHaveParas(paragraphs: String*): Unit =
      for (p <- paragraphs) {
        s"$name must not have the paragraph '$p'" in {
          element.getElementsByTag("p").text() must not include p
        }
      }

    def mustHaveBulletSeq(bullets: String*): Unit = {
      if (bullets.isEmpty) fail("Must provide at least 1 bullet point for this test")
      val bs = bullets.mkString(" ")
      s"$name must have the bulletPoints (LI) [${bullets.mkString("], [")}]" in {
        element.getElementsByTag("LI").text() must include(bs)
      }
    }

    def mustNotHaveBullets(bullets: String*): Unit =
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

    def mustHaveRadioSet(legend: String, radioName: String)(options: RadioOption*): Unit = {
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

    def mustHaveTextField(name: String,
                          label: String,
                          showLabel: Boolean = true,
                          maxLength: Option[Int] = None,
                          pattern: Option[String] = None,
                          inputMode: Option[String] = None,
                          hint: Option[String] = None
                         ): Unit = {

      s"${this.name} must have an input field '$name'" which {

        s"is a text field" in {
          import collection.JavaConversions._
          val eles = element.select(s"""input[name=$name]""")
          if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
          if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
          val ele = eles.head
          ele.attr("type") mustBe "text"
          maxLength.map {
            l => ele.attr("maxLength") mustBe l.toString
          }
          pattern.map {
            p => ele.attr("pattern") mustBe p
          }
          inputMode.map {
            m => ele.attr("inputMode") mustBe m
          }
        }

        lazy val labelField = element.select(s"label[for=$name]")

        s"with the expected label label '$label'" in {
          labelField.text() mustBe (label + hint.fold("")(" " + _))
        }

        if (!showLabel)
          s"and the label must be visuallyhidden" in
            withClue(s"$name does not have the class 'visuallyhidden'\n") {
              labelField.hasClass("visuallyhidden") mustBe true
            }

      }

    }

    def mustHaveHiddenInputField(name: String): Unit =
      s"$name must have input field $name" in {
        import collection.JavaConversions._
        val eles = element.select(s"""input[name="$name"]""")
        if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
        if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
        val ele = eles.head
        ele.tag() mustBe "input"
        ele.attr("type") mustBe "hidden"
      }

    def mustHaveSubmitButton(text: String): Unit =
      s"$name must have the a submit button (Button) '$text'" in {
        import collection.JavaConversions._
        val submitButtons = element.select("button").filter(_.attr("type") == "submit")
        submitButtons.size mustBe 1
        submitButtons.head.text() mustBe text
      }

    def mustHaveContinueButton(): Unit = mustHaveSubmitButton(common.continue)

    def mustHaveUpdateButton(): Unit = mustHaveSubmitButton(common.update)

    def mustHaveCheckbox(name: String, message: String): Unit =
      s"${this.name} must have a checkbox for '$name' with label '$message'" in {
        import collection.JavaConversions._
        val checkbox: Elements = new Elements(element.select("input").filter(x => x.attr("type").equals("checkbox")))
        if (checkbox.size() == 0) fail(s"""Unable to locate any checkboxes in "${this.name}""""")
        if (checkbox.size() > 1) fail(s"""Multiple checkboxes located in "$name", please specify an id""")
        checkbox.attr("name") mustBe name
        checkbox.parents().get(0).text() mustBe message
      }

    def getAccordion(accordionName: String, summary: String): ElementTest = {
      s"$name have the accordion '$accordionName' (details) '$summary'" in {
        element.select("details summary span.summary").text() mustBe summary
      }

      selectHead(accordionName, "details div")
    }

    def mustHaveDateField(id: String, legend: String, exampleDate: String): Unit = {
      val selector = s"#$id"
      s"${this.name} have a fieldset with id '$id' with the legend '$legend'" in {
        val ele = element.getElementById(id)
        ele.select("span.form-label-bold").text() mustBe legend
        ele.select("span.form-hint").text() mustBe exampleDate
        ele.tag().toString mustBe "fieldset"
      }
      val date = selectHead(id, selector)
      val numericPattern = "[0-9]*"
      val inputMode = "numeric"
      date.mustHaveTextField(s"$id.dateDay", common.day, maxLength = 2, pattern = numericPattern, inputMode = inputMode)
      date.mustHaveTextField(s"$id.dateMonth", common.month, maxLength = 2, pattern = numericPattern, inputMode = inputMode)
      date.mustHaveTextField(s"$id.dateYear", common.year, maxLength = 4, pattern = numericPattern, inputMode = inputMode)
    }
  }

  object ElementTest {

    // n.b. element must be null-ary function to prevent evaluation at instantiation
    def apply(name: String, element: () => Element): ElementTest = {
      val n = name
      val ele = element
      if (ele == null) {
        throw new IllegalArgumentException("creation of name failed: element is null")
      }
      new ElementTest {
        override lazy val name: String = n
        override lazy val element: Element = ele()
      }

    }

  }

  // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
  class TestView(override val name: String,
                 title: String,
                 heading: String,
                 page: => Html,
                 showSignOutInBanner: Boolean = true) extends ElementTest {

    lazy val document: Document = Jsoup.parse(page.body)
    override lazy val element: Element = document.getElementById("content")

    if (showSignOutInBanner) {
      s"$name must have a sign out link in the banner" in {
        val signOut = document.getElementById("logOutNavHref")
        if (signOut == null) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
        signOut.text() mustBe common.signOut
        signOut.attr("href") mustBe controllers.routes.SignOutController.signOut().url
      }
    } else {
      s"$name must not have a sign out link in the banner" in {
        val signOut = document.getElementById("logOutNavHref")
        signOut mustBe null
      }
    }

    s"$name must have the title '$title'" in {
      document.title() mustBe title
    }

    s"$name must have the heading (H1) '$heading'" in {
      val h1 = document.getElementsByTag("H1")
      h1.size() mustBe 1
      h1.text() mustBe heading
    }

    def mustHaveBackLinkTo(backUrl: String): Unit =
      s"$name must have a back link pointed to '$backUrl'" in {
        val backLink = element.select("#back")
        backLink.isEmpty mustBe false
        backLink.attr("href") mustBe backUrl
      }

    // this method returns either the first form in the document or one specified by id
    // @param formName the name used to reference the form by the unit tests in its print statements.
    // n.b. the param actionCall must be call-by-name otherwise it could be evaluated with the wrong
    // context root
    def getForm(formName: String, id: Option[String] = None)(actionCall: => Call): ElementTest = {
      val selector =
        id match {
          case Some(i) => s"#$i"
          case _ => "form"
        }

      lazy val method = actionCall.method
      lazy val url = actionCall.url
      // this test is put in here because it doesn't make sense for it to be called on anything
      // other than a form
      s"$formName must must a $method action to '$url'" in {
        val formSelector = element.select(selector)
        formSelector.attr("method") mustBe method.toUpperCase
        formSelector.attr("action") mustBe url
      }

      // csrf token is not tested here because it is only added if the correct headers are set in the request
      selectHead(formName, selector)
    }

  }

  object TestView {
    // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
    def apply(name: String,
              title: String,
              heading: String,
              page: => Html,
              showSignOutInBanner: Boolean = true): TestView = new TestView(name, title, heading, page, showSignOutInBanner)
  }

}


object ViewSpecTrait{
  // these two constants are used for testing the views
  val testBackUrl = "/test-back-url"
  val testCall = Call("POST", "/test-url")
}