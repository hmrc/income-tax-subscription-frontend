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

package views

import assets.MessageLookup
import assets.MessageLookup.{Base => common}
import controllers.SignOutController
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import utilities.UnitTestTrait
import scala.language.implicitConversions

import scala.jdk.CollectionConverters._


trait ViewSpecTrait extends UnitTestTrait {

  val titleErrPrefix: String = MessageLookup.Base.titleError

  case class Selector(name: String, cssQuery: String)

  trait ElementTest {
    val name: String

    val element: Element


    // n.b. should not be made public since it is not a test nor does it return an ElementTest
    protected def getById(id: String): Element = {
      Option(element.getElementById(id)) match {
        case None => fail(s"unable to locate: $id")
        case Some(ele) => ele
      }
    }

    def getById(name: String, id: String): ElementTest = {
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

    def mustHaveH3(text: String): Unit =
      s"$name have a Heading 3 (H3) for '$text'" in {
        element.getElementsByTag("h3").text() must include(text)
      }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        val link = element.select("a")
        if (link.isEmpty) fail(s"Unable to locate any links in $name\n$element\n")
        if (link.size() > 1) fail(s"Multiple links located in $name, please specify an id")
        link.attr("href") mustBe href
        link.text() mustBe text
      }

    // n.b. href must be call-by-name otherwise it may not be evaluated with the correct context-root
    def mustHaveALink(id: String, text: String, href: => String): Unit =
      s"$name have a link with text '$text' pointed to '$href'" in {
        Option(element.getElementById(id)) match {
          case None => fail(s"Unable to locate $id")
          case Some(link) if !link.tagName().equals("a") => fail(s"The element with id=$id is not a link")
          case Some(link) =>
            link.attr("href") mustBe href
            link.text() mustBe text
        }
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

    case class CheckboxOption(name: String, text: String)

    object CheckboxOption {
      implicit def conv(checkboxOption: CheckboxOption): (String, String) = (checkboxOption.name, checkboxOption.text)

      implicit def conv(checkboxOption: Seq[CheckboxOption]): Seq[(String, String)] = checkboxOption.map(x => x: (String, String))

      implicit def conv(checkboxOption: (String, String)): CheckboxOption = CheckboxOption(checkboxOption._1, checkboxOption._2)

      implicit class CheckboxOptionSeqUtil(checkboxOption: Seq[CheckboxOption]) {
        def toTuples: Seq[(String, String)] = checkboxOption: Seq[(String, String)]
      }

    }

    def mustHaveRadioSet(legend: String, radioName: String, useTextForValue: Boolean = false)(options: RadioOption*): Unit = {
      if (legend.isEmpty) fail("Legend cannot be none empty, this would cause an accessibility issue")
      if (radioName.isEmpty) fail("Must provide the field name which groups all the buttons in this test")
      if (options.isEmpty) fail("Must provide at least 1 radio button for this test")
      if (options.size == 1) fail("It does not make sense to have a radio button fieldset with only a single option")

      s"$name must have a radio fieldset for $legend" which {

        s"has a legend with the text '$legend'" in {
          element.select(s"fieldset legend").text() mustBe legend
        }

        for ((radio, index) <- options.zip(1 to options.length)) {
          val ignoredFirstIndex = if (index == 1) "" else s"-$index"
          s"has a radio option for '$radioName-$index'" in {
            val radioButton = element.select(s"#$radioName$ignoredFirstIndex")
            radioButton.attr("type") mustBe "radio"
            radioButton.attr("name") mustBe s"$radioName"
            useTextForValue match {
              case true => radioButton.attr("value") mustBe radio.text
              case false => radioButton.attr("value") mustBe radio.name
            }
            val label = element.getElementsByAttributeValue("for", s"$radioName$ignoredFirstIndex")
            label.size() mustBe 1
            label.get(0).text() mustBe radio.text
          }
        }

      }
    }

    def mustHaveCheckboxSet(legend: String, checkboxName: String, useTextForValue: Boolean = false)(labels: String*): Unit = {
      if (legend.isEmpty) fail("Legend cannot be none empty, this would cause an accessibility issue")
      if (checkboxName.isEmpty) fail("Must provide the field name which groups all the checkboxes in this test")
      if (labels.isEmpty) fail("Must provide at least 1 checkbox for this test")
      if (labels.size == 1) fail("It does not make sense to have a checkbox fieldset with only a single checkbox")

      s"$name must have a checkbox fieldset for $legend" which {

        s"has a legend with the text '$legend'" in {
          element.select(s"fieldset legend[id=$checkboxName]").text() mustBe legend
        }

        for ((text, index) <- labels.zipWithIndex) {
          s"has a radio option for '$checkboxName-$index'" in {
            val checkbox = element.select(s"#$checkboxName-$index")
            checkbox.attr("type") mustBe "checkbox"
            checkbox.attr("name") mustBe s"$checkboxName[$index]"
            useTextForValue match {
              case true => checkbox.attr("value") mustBe text
              case false => checkbox.attr("value") mustBe "true"
            }
            val label = element.getElementsByAttributeValue("for", s"$checkboxName-$index")
            label.size() mustBe 1
            label.get(0).text() mustBe text
          }
        }

      }
    }

    def mustHaveTextArea(name: String,
                         label: String,
                         showLabel: Boolean,
                         maxLength: Option[Int] = None
                        ): Unit = {
      s"${this.name} must have a textarea field '$name'" which {
        s"is a text area" in {
          val eles = element.select(s"""textarea[name=$name]""").asScala
          if (eles.isEmpty) fail(s"$name does not have an text area with name=$name\ncurrent list of textareas:\n[${element.select("textarea")}]")
          if (eles.size > 1) fail(s"$name have multiple text areas with name=$name")
          val ele = eles.head
          maxLength match {
            case Some(l) =>
              ele.attr("maxLength") mustBe l.toString
            case _ =>
              ele.hasAttr("maxLength") mustBe false
          }
          val labelTag = element.select(s"label[for=$name]")
          labelTag.text() mustBe label
          labelTag.hasClass("hidden") mustBe !showLabel
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

        val eles = element.select(s"""input[name=$name]""")

        s"is a text field" in {
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
          labelField.text() mustBe (label)
        }

        if (hint.isDefined)
          s"with the expected linked hint text '$hint" in {
            val hintId = eles.attr("aria-describedby")
            element.select(s"*[id=$hintId]").text() mustBe hint.get
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
        val eles = element.select(s"""input[name="$name"]""")
        if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
        if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
        val ele = eles.head
        ele.tag() mustBe "input"
        ele.attr("type") mustBe "hidden"
      }

    def mustHaveSubmitButton(text: String): Unit =
      s"$name must have the a submit button (Button) '$text'" in {
        val submitButtons = element.select("button")
        submitButtons.size mustBe 1
        submitButtons.head.text() mustBe text
      }

    def mustHaveSignOutButton(text: String, optOrigin: Option[String] = None): Unit =
      s"$name must have the a sign out button (a) '$text'" in {
        val signOutButton = element.getElementById("sign-out-button")
        signOutButton.attr("role") mustBe "button"
        signOutButton.text() mustBe text
        optOrigin match {
          case Some(origin) => signOutButton.attr("href") mustBe SignOutController.signOut.url
          case _ => None
        }
      }

    def mustHaveSignOutLink(text: String, optOrigin: Option[String] = None): Unit = {
      val id = "sign-out"
      optOrigin match {
        case Some(origin) => mustHaveALink(id, text, SignOutController.signOut.url)
        case _ =>
          s"$name have a link with text '$text' pointed to 'Sign Out'" in {
            Option(element.getElementById(id)) match {
              case None => fail(s"Unable to locate $id")
              case Some(link) =>
                if (!link.tagName().equals("a")) fail(s"The element with id=$id is not a link")
                link.text() mustBe text
            }
          }
      }
    }

    def mustHaveSignOutLinkGovUk(text: String, optOrigin: Option[String] = None): Unit = {
      val id = "sign-out-button"
      optOrigin match {
        case Some(origin) => mustHaveALink(id, text, SignOutController.signOut.url)
        case _ =>
          s"$name have a link with text '$text' pointed to 'Sign Out'" in {
        Option(element.getElementById(id)) match {
          case None => fail(s"Unable to locate $id")
          case Some(link) if (!link.tagName().equals("a")) => fail(s"The element with id=$id is not a link")
          case Some(link) => link.text() mustBe text
        }
          }
      }
    }

    def mustHaveContinueButton(): Unit = mustHaveSubmitButton(common.continue)

    def mustHaveContinueToSignUpButton(): Unit = mustHaveSubmitButton(common.continueToSignUp)

    def mustHaveUpdateButton(): Unit = mustHaveSubmitButton(common.update)

    def mustHaveGoBackButton(): Unit = mustHaveSubmitButton(common.goBack)

    def mustHaveContinueButtonWithText(text: String): Unit = {
      s"$name must have a button with id 'continue-button' with text '$text'" in {
        element.getElementById("continue-button").text() mustBe (text)
      }
    }

    def mustHaveCheckboxWithId(id: String, name: String, message: String): Unit =
      s"${this.name} must have a checkbox for '$name' with label '$message'" in {
        val checkbox: Element = element.getElementById(id)
        checkbox.attr("type") mustBe "checkbox"
        checkbox.attr("name") mustBe name
        checkbox.parents().get(0).text() mustBe message
      }

    def mustHaveCheckbox(name: String, message: String): Unit =
      s"${this.name} must have a checkbox for '$name' with label '$message'" in {
        val elements = element.select("input")
          .asScala
          .filter(x => x.attr("type").equals("checkbox"))
          .asJava
        val checkbox: Elements = new Elements(elements)
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

    def mustHaveDateField(id: String, legend: String, exampleDate: String, isPageHeading: Boolean = true): Unit = {
      val selector = s"#$id"
      s"${this.name} have a fieldset with class 'govuk-fieldset' with the legend '$legend'" in {
        val fieldset = element.getElementsByClass("govuk-fieldset")

        if (isPageHeading) {
          fieldset.select("legend").select("h1").text mustBe legend
        } else {
          fieldset.select("legend").text() mustBe legend
        }
        fieldset.select("div#clientDateOfBirth-hint").text() mustBe exampleDate
        fieldset.first().tag().toString mustBe "fieldset"
      }
      val date = selectHead(id, selector)
      val numericPattern = Some("[0-9]*")
      val inputMode = Some("numeric")
      date.mustHaveTextField(s"$id-dateDay", common.day, pattern = numericPattern, inputMode = inputMode)
      date.mustHaveTextField(s"$id-dateMonth", common.month, pattern = numericPattern, inputMode = inputMode)
      date.mustHaveTextField(s"$id-dateYear", common.year, pattern = numericPattern, inputMode = inputMode)
    }


    def mustHaveHrefValue(id: String, href: String): Unit = {
      s"${this.name} have a href attribute value with id '$id' " in {
        val ele = element.getElementById(id)
        ele.attr("href") mustBe href
      }

    }
  }

  object ElementTest {

    // n.b. element must be null-ary function to prevent evaluation at instantiation
    def apply(name: String, element: () => Element): ElementTest = {
      val n = name
      Option(element) match {
        case None => throw new IllegalArgumentException("creation of name failed: element is Empty")
        case Some(ele) =>
          new ElementTest {
            override lazy val name: String = n
            override lazy val element: Element = ele()
          }
      }
    }

  }

  // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
  class TestView(override val name: String,
                 title: String,
                 heading: String,
                 page: => Html,
                 showSignOutInBanner: Boolean = true,
                 isAgent: Boolean = false) extends ElementTest {

    lazy val document: Document = Jsoup.parse(page.body)
    override lazy val element: Element = Option(document.getElementById("content")).getOrElse(document.getElementById("main-content"))

    if (showSignOutInBanner) {
      s"$name must have a sign out link in the banner" in {
        val signOut = Option(document.getElementById("logOutNavHref")).orElse(Option(document.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
        if (signOut.isEmpty) fail("Signout link was not located in the banner\nIf this is the expected behaviour then please set 'signOutInBanner' to true when creating the TestView object")
        signOut.get.text() mustBe common.signOut
        signOut.get.attr("href") must startWith(controllers.SignOutController.signOut.url)
      }
    } else {
      s"$name must not have a sign out link in the banner" in {
        val signOut = Option(document.getElementById("logOutNavHref")).orElse(Option(document.select(".hmrc-sign-out-nav__link")).filter(e => !e.isEmpty).map(e => e.get(0)))
        signOut mustBe None
      }
    }

    if (isAgent) {
      s"$name must have the title '$title'" in {
        val agentServiceNameGovUK = " - Use software to report your client’s Income Tax - GOV.UK"
        document.title() mustBe title + agentServiceNameGovUK
      }
    } else {
      s"$name must have the title '$title'" in {
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        document.title() mustBe title + serviceNameGovUk
      }
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
      s"$formName in $name must have a $method action to '$url'" in {
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
              isAgent: Boolean = false,
              showSignOutInBanner: Boolean = true): TestView = new TestView(name, title, heading, page, showSignOutInBanner, isAgent)
  }

  implicit class FormUtil[T](form: Form[T]) {
    def addError(addError: Boolean): Form[T] = if (addError) form.withError("test", "test", "err") else form
  }

}


object ViewSpecTrait {
  // these two constants are used for testing the views
  val testBackUrl = "/test-back-url"
  val testCall = Call("POST", "/test-url")
  val viewTestRequest = FakeRequest("POST", "/test-url")
}
