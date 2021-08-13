/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.{Assertion, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html

import scala.collection.JavaConversions._

trait ViewSpec extends WordSpec with MustMatchers with GuiceOneAppPerSuite {

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val mockMessages: Messages = messagesApi.preferred(FakeRequest())

  val testBackUrl = "/test-back-url"
  val testCall: Call = Call("POST", "/test-url")

  implicit val request: Request[_] = FakeRequest()

  class TemplateViewTest(view: Html,
                         title: String,
                         isAgent: Boolean = false,
                         backLink: Option[String] = None,
                         hasSignOutLink: Boolean = false,
                         error: Option[FormError] = None) {

    val document: Document = Jsoup.parse(view.body)

    private val titlePrefix: String = if (error.isDefined) "Error: " else ""
    private val titleSuffix: String = if (isAgent) {
      " - Use software to report your client’s Income Tax - GOV.UK"
    } else {
      " - Use software to send Income Tax updates - GOV.UK"
    }

    document.title mustBe s"$titlePrefix$title$titleSuffix"

    backLink.map { href =>
      val link = document.selectHead(".govuk-back-link")
      link.text mustBe "Back"
      link.attr("href") mustBe href
    }

    if (hasSignOutLink) {
      val signOutLink: Element = document.selectHead(".hmrc-sign-out-nav__link")
      signOutLink.text mustBe "Sign out"
      signOutLink.attr("href") mustBe controllers.routes.SignOutController.signOut().url
    } else {
      document.selectOptionally(".hmrc-sign-out-nav__link") mustBe None
    }

    error.map { formError =>
      val errorSummary: Element = document.selectHead(".govuk-error-summary")
      errorSummary.selectHead("h2").text mustBe "There is a problem"
      val errorLink: Element = errorSummary.selectHead("div > ul > li > a")
      errorLink.text mustBe formError.message
      errorLink.attr("href") mustBe s"#${formError.key}"
    }

  }

  implicit class CustomSelectors(element: Element) {

    def selectHead(selector: String): Element = {
      element.select(selector).headOption match {
        case Some(element) => element
        case None => fail(s"No elements returned for selector: $selector")
      }
    }

    def selectOptionally(selector: String): Option[Element] = {
      element.select(selector).headOption
    }

    def selectNth(selector: String, nth: Int): Element = {
      selectHead(s"$selector:nth-of-type($nth)")
    }

    def content: Element = element.selectFirst("article")

    def mainContent: Element = element.selectFirst("main")

    def getParagraphs: Elements = element.getElementsByTag("p")

    def getNthParagraph(nth: Int): Element = element.selectFirst(s"p:nth-of-type($nth)")

    def getNthUnorderedList(nth: Int): Element = element.selectFirst(s"ul:nth-of-type($nth)")

    def getNthListItem(nth: Int): Element = element.selectFirst(s"li:nth-of-type($nth)")

    def getBulletPoints: Elements = element.getElementsByTag("li")

    def getH1Element: Element = element.selectFirst("h1")

    def getH2Elements: Elements = element.getElementsByTag("h2")

    def getFormElements: Elements = element.getElementsByClass("form-field-group")

    def getErrorSummaryMessage: Elements = element.select("#error-summary-display ul")

    def getErrorSummary: Elements = element.select("#error-summary-display")

    def getSubmitButton: Element = element.selectFirst("button[type=submit]")

    def getHintText: String = element.select(s"""[class=form-hint]""").text()

    def getForm: Element = element.selectFirst("form")

    def getFieldset: Element = element.selectFirst("fieldset")

    def getBackLink: Elements = element.select(s"a[class=link-back]")

    def getParagraphNth(index: Int = 0): String = {
      element.select("p").get(index).text()
    }

    def getRadioButtonByIndex(index: Int = 0): Element = element.select("div .multiple-choice").get(index)

    def getSpan(id: String): Elements = element.select(s"""span[id=$id]""")

    def getLink(id: String): Element = element.selectFirst(s"""a[id=$id]""")

    def getTextFieldInput(id: String): Elements = element.select(s"""input[id=$id]""")

    def getFieldErrorMessage(id: String): Elements = element.select(s"""a[id=$id-error-summary]""")

    //Check your answers selectors
    def getSummaryList: Element = element.selectFirst("dl.govuk-summary-list")

    def getSummaryListRow(nth: Int): Element = {
      element.selectFirst(s"div.govuk-summary-list__row:nth-of-type($nth)")
    }

    def getSummaryListKey: Element = element.selectFirst("dt.govuk-summary-list__key")

    def getSummaryListValue: Element = element.selectFirst("dd.govuk-summary-list__value")

    def getSummaryListActions: Element = element.selectFirst("dd.govuk-summary-list__actions")

  }

  implicit class ElementTests(element: Element) {

    def mustHaveTextField(name: String, label: String): Assertion = {
      val eles = element.select(s"input[name=$name]")
      if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
      if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
      val ele = eles.head
      ele.attr("type") mustBe "text"
      element.select(s"label[for=$name]").text() mustBe label
    }

    def listErrorMessages(errors: List[String]): Assertion = {
      errors.zipWithIndex.map {
        case (error, index) => element.select(s"span.error-notification:nth-child(${index + 1})").text mustBe error
      } forall (_ == succeed) mustBe true
    }

    def mustHaveDateField(id: String, legend: String, exampleDate: String, error: Option[String] = None, isPageHeading: Boolean = true): Assertion = {
      val fieldset: Element = element.selectHead("fieldset")

      fieldset.attr("aria-describedby") mustBe s"$id-hint" + error.map(_ => s" $id-error").getOrElse("")

      if (isPageHeading) {
        fieldset.selectHead("fieldset").selectHead("legend").selectHead("h1").text mustBe legend
      } else {
        fieldset.selectHead("fieldset").selectHead("legend").selectHead("div.form-label-bold").text() mustBe legend
      }

      fieldset.selectHead("fieldset").select("div.form-hint").text() mustBe exampleDate

      fieldset.mustHaveTextField(s"$id.dateDay", "Day")
      fieldset.mustHaveTextField(s"$id.dateMonth", "Month")
      fieldset.mustHaveTextField(s"$id.dateYear", "Year")

      error.map { message =>
        fieldset.select("fieldset").select("div.error-notification").text mustBe s"Error: $message"
      }.getOrElse(succeed)
    }

    def mustHavePara(paragraph: String): Assertion = {
      element.getElementsByTag("p").text() must include(paragraph)
    }

    def mustHaveErrorSummary(errors: List[String]): Assertion = {
      element.getErrorSummary.attr("class") mustBe "flash error-summary error-summary--show"
      element.getErrorSummary.attr("role") mustBe "alert"
      element.getErrorSummary.attr("aria-labelledby") mustBe "error-summary-heading"
      element.getErrorSummary.attr("tabindex") mustBe "-1"
      element.getErrorSummary.select("h2").attr("id") mustBe "error-summary-heading"
      element.getErrorSummary.select("h2").text mustBe "There is a problem"
      element.getErrorSummary.select("ul > li").text mustBe errors.mkString(" ")
    }


  }

}
