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

package utilities

import config.AppConfig
import config.featureswitch.FeatureSwitching
import models.{No, Yes}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.{Assertion, BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import scala.collection.JavaConverters._

trait ViewSpec extends WordSpec with MustMatchers with GuiceOneAppPerSuite with BeforeAndAfterEach with FeatureSwitching {

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
                         hasSignOutLink: Boolean = true,
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
      signOutLink.attr("href") mustBe controllers.routes.SignOutController.signOut.url
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
      element.select(selector).asScala.headOption match {
        case Some(element) => element
        case None => fail(s"No elements returned for selector: $selector")
      }
    }

    def selectOptionally(selector: String): Option[Element] = {
      element.select(selector).asScala.headOption
    }

    def selectNth(selector: String, nth: Int): Element = {
      selectHead(s"$selector:nth-of-type($nth)")
    }

    def content: Element = element.selectHead("article")

    def mainContent: Element = element.selectHead("main")

    def getParagraphs: Elements = element.getElementsByTag("p")

    def getNthParagraph(nth: Int): Element = element.selectHead(s"p:nth-of-type($nth)")

    def getNthUnorderedList(nth: Int): Element = element.selectHead(s"ul:nth-of-type($nth)")

    def getNthListItem(nth: Int): Element = element.selectHead(s"li:nth-of-type($nth)")

    def getBulletPoints: Elements = element.getElementsByTag("li")

    def getH1Element: Element = element.selectHead("h1")

    def getH2Elements: Elements = element.getElementsByTag("h2")

    def getFormElements: Elements = element.getElementsByClass("form-field-group")

    def getErrorSummaryMessage: Elements = element.select("#error-summary-display ul")

    def getErrorSummary: Elements = element.select("#error-summary-display")

    def getGovukErrorSummary: Elements = element.select(".govuk-error-summary")

    def getSubmitButton: Element = element.selectHead("button[type=submit]")

    def getGovukSubmitButton: Element = element.selectHead(".govuk-button")

    def getHintText: String = element.select(s"""[class=form-hint]""").text()

    def getForm: Element = element.selectHead("form")

    def getFieldset: Element = element.selectHead("fieldset")

    def getBackLink: Elements = element.select(s"a[class=link-back]")

    def getGovukBackLink: Elements = element.select("a[class=govuk-back-link]")

    def getParagraphNth(index: Int = 0): String = {
      element.select("p").get(index).text()
    }

    def getLinkNth(index: Int = 0): Element = {
      element.select(".govuk-link").get(index)
    }

    def getRadioButtonByIndex(index: Int = 0): Element = element.select("div .multiple-choice").get(index)

    def getSpan(id: String): Elements = element.select(s"""span[id=$id]""")

    def getLink(id: String): Element = element.selectHead(s"""a[id=$id]""")

    def getTextFieldInput(id: String): Elements = element.select(s"""input[id=$id]""")

    def getFieldErrorMessage(id: String): Elements = element.select(s"""a[id=$id-error-summary]""")

    //Check your answers selectors
    def getSummaryList: Element = element.selectHead("dl.govuk-summary-list")

    def getSummaryListRow(nth: Int): Element = {
      element.selectHead(s"div.govuk-summary-list__row:nth-of-type($nth)")
    }

    def getSummaryListKey: Element = element.selectHead("dt.govuk-summary-list__key")

    def getSummaryListValue: Element = element.selectHead("dd.govuk-summary-list__value")

    def getSummaryListActions: Element = element.selectHead("dd.govuk-summary-list__actions")

  }

  implicit class ElementTests(element: Element) {

    def mustHaveRadioInput(name: String, radioItems: Seq[RadioItem]): Assertion = {
      radioItems.zip(1 to radioItems.length) map { case (radioItem, index) =>
        val radioElement: Element = element.selectNth(".govuk-radios__item", index)
        val radioInput: Element = radioElement.selectHead("input")
        radioItem.id mustBe Some(radioInput.attr("id"))
        radioInput.attr("name") mustBe name
        radioInput.attr("type") mustBe "radio"
        Some(radioInput.attr("value")) mustBe radioItem.value

        val radioLabel: Element = radioElement.selectHead("label")
        radioLabel.attr("for") mustBe radioInput.attr("id")
        Text(radioLabel.text) mustBe radioItem.content
      } forall (_ == succeed) mustBe true
    }

    def mustHaveYesNoRadioInputs(name: String): Assertion = {
      mustHaveRadioInput(
        name = name,
        radioItems = Seq(
          RadioItem(
            id = Some(name),
            content = Text(Yes.toMessageString),
            value = Some(Yes.toString)
          ),
          RadioItem(
            id = Some(s"$name-2"),
            content = Text(No.toMessageString),
            value = Some(No.toString)
          )
        )
      )
    }

    def mustHaveTable(tableHeads: List[String], tableRows: List[List[String]]): Assertion = {
      val table: Element = element.selectHead("table")

      tableHeads.zip(1 to tableHeads.length).map { case (th, index) =>
        table.selectHead("thead").selectHead("tr").selectNth("th", index).text mustBe th
      } forall (_ == succeed) mustBe true

      tableRows.zip(1 to tableRows.length).map { case (tr, index) =>
        val tableRow = table.selectHead("tbody").selectNth("tr", index)
        tr.zip(1 to tr.length).map { case (td, index) =>
          tableRow.selectNth("td", index).text mustBe td
        } forall (_ == succeed)
      } forall (_ == true) mustBe true
    }

    def mustHaveTextInput(name: String,
                          label: String,
                          hint: Option[String] = None,
                          error: Option[FormError] = None,
                          autoComplete: Option[String] = None): Assertion = {
      val textInput: Element = element.selectHead(s"input[name=$name]")
      val textInputLabel: Element = element.selectHead(s"label[for=$name]")

      textInputLabel.text mustBe label

      autoComplete.foreach(value => textInput.attr("autocomplete") mustBe value)

      hint.foreach { value =>
        element.selectHead(s"#$name-hint").text mustBe value
        textInput.attr("aria-describedby").contains(s"$name-hint") mustBe true
      }

      error.foreach { value =>
        element.selectHead(s"#${value.key}-error").text mustBe s"Error: ${value.message}"
        textInput.attr("aria-describedby").contains(s"${value.key}-error") mustBe true
      }

      textInput.attr("type") mustBe "text"
    }

    def mustHaveDateInput(name: String,
                          label: String,
                          hint: Option[String] = None,
                          error: Option[FormError] = None,
                          isDateOfBirth: Boolean = false): Assertion = {

      val fieldset: Element = element.selectHead("fieldset")
      val legend: Element = element.selectHead("legend")

      legend.text mustBe label

      hint.foreach { value =>
        element.selectHead(s"#$name-hint").text mustBe value
        fieldset.attr("aria-describedby").contains(s"$name-hint") mustBe true
      }

      error.foreach { value =>
        element.selectHead(s"#${value.key}-error").text mustBe s"Error: ${value.message}"
        fieldset.attr("aria-describedby").contains(s"${value.key}-error") mustBe true
      }

      val dayInput: Element = fieldset.selectNth(".govuk-date-input__item", 1).selectHead("input")
      val dayLabel: Element = fieldset.selectNth(".govuk-date-input__item", 1).selectHead("label")
      val monthInput: Element = fieldset.selectNth(".govuk-date-input__item", 2).selectHead("input")
      val monthLabel: Element = fieldset.selectNth(".govuk-date-input__item", 2).selectHead("label")
      val yearInput: Element = fieldset.selectNth(".govuk-date-input__item", 3).selectHead("input")
      val yearLabel: Element = fieldset.selectNth(".govuk-date-input__item", 3).selectHead("label")

      dayInput.attr("name") mustBe s"$name-dateDay"
      dayInput.attr("type") mustBe "text"
      dayInput.attr("pattern") mustBe "[0-9]*"
      dayInput.attr("inputmode") mustBe "numeric"
      if (isDateOfBirth) dayInput.attr("autocomplete") mustBe "bday-day"
      dayLabel.text mustBe "Day"
      dayLabel.attr("for") mustBe s"$name-dateDay"

      monthInput.attr("name") mustBe s"$name-dateMonth"
      monthInput.attr("type") mustBe "text"
      monthInput.attr("pattern") mustBe "[0-9]*"
      monthInput.attr("inputmode") mustBe "numeric"
      if (isDateOfBirth) monthInput.attr("autocomplete") mustBe "bday-month"
      monthLabel.text mustBe "Month"
      monthLabel.attr("for") mustBe s"$name-dateMonth"

      yearInput.attr("name") mustBe s"$name-dateYear"
      yearInput.attr("type") mustBe "text"
      yearInput.attr("pattern") mustBe "[0-9]*"
      yearInput.attr("inputmode") mustBe "numeric"
      if (isDateOfBirth) yearInput.attr("autocomplete") mustBe "bday-year"
      yearLabel.text mustBe "Year"
      yearLabel.attr("for") mustBe s"$name-dateYear"

    }

    def mustHaveTextField(name: String, label: String): Assertion = {
      val eles = element.select(s"input[name=$name]")
      if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
      if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
      val ele = eles.asScala.head
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

    def mustHaveGovukDateField(id: String, legend: String, exampleDate: String, error: Option[String] = None): Assertion = {
      val fieldset: Element = element.selectHead("fieldset")

      fieldset.attr("aria-describedby") mustBe s"$id-hint" + error.map(_ => s" $id-error").getOrElse("")

      fieldset.selectHead("fieldset").selectHead("legend").text() mustBe legend

      fieldset.selectHead("fieldset").select(".govuk-hint").text() mustBe exampleDate

      fieldset.mustHaveTextField(s"$id-dateDay", "Day")
      fieldset.mustHaveTextField(s"$id-dateMonth", "Month")
      fieldset.mustHaveTextField(s"$id-dateYear", "Year")

      error.map { message =>
        fieldset.select("fieldset").select(".govuk-error-message").text mustBe s"Error: $message"
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

    def mustHaveGovukErrorSummary(error: String): Assertion = {
      element.getGovukErrorSummary.attr("role") mustBe "alert"
      element.getGovukErrorSummary.attr("aria-labelledby") mustBe "error-summary-title"
      element.getGovukErrorSummary.attr("tabindex") mustBe "-1"
      element.getGovukErrorSummary.select("h2").attr("id") mustBe "error-summary-title"
      element.getGovukErrorSummary.select("h2").text mustBe "There is a problem"
      element.getGovukErrorSummary.select("ul > li a").text mustBe error
    }
  }

}
