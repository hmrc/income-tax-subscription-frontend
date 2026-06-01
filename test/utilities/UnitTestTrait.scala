/*
 * Copyright 2023 HM Revenue & Customs
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

package utilities

import config.featureswitch.FeatureSwitching
import config.{AppConfig, MockConfig}
import controllers.SignOutController
import messagelookup.individual.MessageLookup
import messagelookup.individual.MessageLookup.Base as common
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Environment
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Call, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.twirl.api.Html
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait UnitTestTrait extends PlaySpec
  with GuiceOneServerPerSuite
  with I18nSupport
  with FeatureSwitching
  with OptionValues
  with AnyWordSpecLike {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit def futureWrapperUtil[T](value: T): Future[T] = Future.successful(value)

  implicit def futureWrapperUtil[T](err: Throwable): Future[T] = Future.failed(err)

  implicit def futureOptionWrapperUtil[T](value: T): Future[Option[T]] = Future.successful(Option(value))

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  implicit val appConfig: AppConfig = MockConfig

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val env: Environment = app.injector.instanceOf[Environment]

  implicit lazy val mockMessages: Messages = messagesApi.preferred(FakeRequest())

  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  trait ElementTest {
    val name: String

    val element: Element

    def selectHead(name: String, cssQuery: String): ElementTest = {
      lazy val n = s"""${this.name}."$name""""
      ElementTest(n, () => {
        val selector = element.select(cssQuery)
        if (selector.isEmpty) fail(s"Unable to locate $cssQuery in\n$element")
        selector.get(selector.size() match {
          case 1 => 0
          case _ => 1
        })
      })
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

    def mustHaveBullets(bullets: String*): Unit = {
      if (bullets.isEmpty) fail("Must provide at least 1 bullet point for this test")
      val bs = bullets.mkString(" ")
      s"$name must have the bulletPoints (LI) [${bullets.mkString("], [")}]" in {
        element.getElementsByTag("LI").text() must include(bs)
      }
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

    def mustHaveSignInButton(text: String, href: String): Unit =
      s"$name must have a link button '$text'" in {
        val signInButton = element.select("a.govuk-button")

        signInButton.size mustBe 1
        signInButton.head.text() mustBe text
        signInButton.head.attr("href") mustBe href
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
  }

  object ElementTest {

    // n.b. element must be null-ary function to prevent evaluation at instantiation
    def apply(name: String, element: () => Element): ElementTest = {
      val n = name
      Option(element) match {
        case None => throw new IllegalArgumentException("creation of name failed: element is Empty")
        case Some(ele) =>
          new ElementTest {
            override val name: String = n
            override val element: Element = ele()
          }
      }
    }
  }

  class TestView(
    override val name: String,
    title: String,
    heading: String,
    page: => Html,
    showSignOutInBanner: Boolean = true,
    isAgent: Boolean = false
  ) extends ElementTest {

    lazy val document: Document = Jsoup.parse(page.body)
    override val element: Element = Option(document.getElementById("content")).getOrElse(document.getElementById("main-content"))

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
        val agentServiceNameGovUK = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
        document.title() mustBe title + agentServiceNameGovUK
      }
    } else {
      s"$name must have the title '$title'" in {
        val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
        document.title() mustBe title + serviceNameGovUk
      }
    }

    s"$name must have the heading (H1) '$heading'" in {
      val h1 = document.getElementsByTag("H1")
      h1.size() mustBe 1
      h1.text() mustBe heading
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
}

object UnitTestTrait {
  // these two constants are used for testing the views
  val testCall = Call("POST", "/test-url")
  val viewTestRequest = FakeRequest("POST", "/test-url")
}
