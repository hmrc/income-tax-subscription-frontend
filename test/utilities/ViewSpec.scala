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

import config.AppConfig
import config.featureswitch.FeatureSwitching
import controllers.SignOutController
import messagelookup.individual.MessageLookup.Base as common
import models.{No, UpdateDeadline, Yes}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Checkpoints.Checkpoint
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.{Assertion, BeforeAndAfterEach, Succeeded}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.LocalDate
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

trait ViewSpec extends UnitTestTrait with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach with FeatureSwitching {

  override implicit lazy val app: Application = fakeApplication()

  override implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val wrappedMessages: Messages = MessagesWrapper(Lang("en"), messagesApi)

  val testCall: Call = Call("POST", "/test-url")

  implicit val request: Request[_] = FakeRequest()

  class TemplateViewTest(view: Html,
                         title: String,
                         isAgent: Boolean = false,
                         hasBackLink: Boolean = true,
                         hasSignOutLink: Boolean = true,
                         error: Option[FormError] = None) {

    val document: Document = Jsoup.parse(view.body)

    private val titlePrefix: String = if (error.isDefined) "Error: " else ""
    private val titleSuffix: String = if (isAgent) {
      " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
    } else {
      " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
    }

    document.title mustBe s"$titlePrefix$title$titleSuffix"

    if (hasSignOutLink) {
      val signOutLink: Element = document.selectHead(".hmrc-sign-out-nav__link")
      signOutLink.text mustBe "Sign out"
      signOutLink.attr("href") mustBe controllers.routes.SignOutController.signOut.url
    } else {
      document.selectOptionally(".hmrc-sign-out-nav__link") mustBe None
    }

    if (hasBackLink) {
      val backLink = document.selectHead(".govuk-back-link")
      backLink.text mustBe "Back"
      backLink.attr("href") mustBe "#"
      backLink.attr("data-module") mustBe "hmrc-back-link"
    } else {
      document.selectOptionally(".govuk-back-link") mustBe None
    }

    error.map { formError =>
      val errorSummary: Element = document.selectHead(".govuk-error-summary")
      errorSummary.selectHead("h2").text mustBe "There is a problem"
      val errorLink: Element = errorSummary.selectHead("div > ul > li > a")
      errorLink.text mustBe wrappedMessages(formError.message, formError.args: _*)
      errorLink.attr("href") mustBe s"#${formError.key}"
    }

  }

  def method(method: String): HavePropertyMatcher[Element, String] =
    (element: Element) => HavePropertyMatchResult(
      element.attr("method") == method,
      "method",
      method,
      element.attr("method")
    )

  def action(action: String): HavePropertyMatcher[Element, String] =
    (element: Element) => HavePropertyMatchResult(
      element.attr("action") == action,
      "action",
      action,
      element.attr("action")
    )


  implicit class CustomSelectors(element: Element) {

    def selectHead(selector: String): Element = {
      selectSeq(selector).headOption match {
        case Some(element) => element
        case None => fail(s"No elements returned for selector: $selector")
      }
    }

    def selectOptionally(selector: String): Option[Element] = {
      selectSeq(selector).headOption
    }

    def selectOptionalNth(selector: String, nth: Int): Option[Element] = {
      selectSeq(selector).lift(nth - 1)
    }

    def selectSeq(selector: String): Seq[Element] = {
      element.select(selector).asScala.toSeq
    }

    def selectNth(selector: String, nth: Int): Element = {
      selectSeq(selector).lift(nth - 1) match {
        case Some(e) => e
        case None => fail(s"Could not retrieve $selector number $nth")
      }
    }

    def content: Element = element.selectHead("article")

    def mainContent: Element = element.selectHead("main")

    def getParagraphs: Elements = element.getElementsByTag("p")

    def getNthParagraph(nth: Int): Element = selectNth("p", nth)

    def getNthUnorderedList(nth: Int): Element = element.selectHead(s"ul:nth-of-type($nth)")

    def getNthListItem(nth: Int): Element = element.selectHead(s"li:nth-of-type($nth)")

    def getBulletPoints: Elements = element.select("ul.govuk-list.govuk-list--bullet")

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

    def getParagraphNth(index: Int = 0): String = {
      element.select("p").get(index).text()
    }

    def getSubHeading(selector: String, nth: Int = 0): Element = {
      val captionExists = element.selectOptionally("h2.govuk-caption-l").isDefined
      mainContent.selectNth(selector, if (captionExists) nth + 1 else nth)
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

  case class TaskListItemValues(text: String, link: Option[String], hint: Option[String], tagText: String, tagColor: Option[String])

  case class SummaryListActionValues(href: String, text: String, visuallyHidden: String)

  case class SummaryListRowValues(key: String, value: Option[String], actions: Seq[SummaryListActionValues])

  case class DateInputFieldValues(label: String, value: Option[String])

  implicit class ElementTests(element: Element) {

    def mustHaveCaption(caption: String, isSection: Boolean): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()

      if (isSection) {
        checkpoint {
          element.selectHead("h2.govuk-caption-l").text mustBe s"This section is $caption"
        }
        checkpoint {
          element.selectHead("h2.govuk-caption-l").selectHead("span.govuk-visually-hidden").text mustBe "This section is"
        }
      } else {
        checkpoint {
          element.selectHead("h2.govuk-caption-l").text mustBe caption
        }
      }

      checkpoint.reportAll()
      Succeeded
    }

    def mustHaveHeadingAndCaption(heading: String, caption: String, isSection: Boolean): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()

      checkpoint {
        element.selectHead("h1.govuk-heading-l").text mustBe heading
      }

      if (isSection) {
        checkpoint {
          element.selectHead("h2.govuk-caption-l").text mustBe s"This section is $caption"
        }
        checkpoint {
          element.selectHead("h2.govuk-caption-l").selectHead("span.govuk-visually-hidden").text mustBe "This section is"
        }
      } else {
        checkpoint {
          element.selectHead("h2.govuk-caption-l").text mustBe caption
        }
      }

      checkpoint.reportAll()
      Succeeded
    }

    //scalastyle:off
    def mustHaveSummaryCard(selector: String, nth: Option[Int] = None)
                           (title: String, cardActions: Seq[SummaryListActionValues], rows: Seq[SummaryListRowValues]): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()

      val card: Element = nth match {
        case Some(number) if number == 0 => fail(s"Invalid nth selector of $number, must be >= 1")
        case Some(number) => element
          .selectSeq(selector)
          .lift(number - 1)
          .getOrElse(fail(s"No elements returned for selector: $selector number $number"))
        case None => element
          .selectHead(selector)
      }


      val titleWrapper: Element = card.selectHead(".govuk-summary-card__title-wrapper")

      checkpoint {
        titleWrapper.selectHead("h3").text mustBe title
      }

      cardActions match {
        case Nil =>
          checkpoint {
            titleWrapper.selectOptionally(".govuk-summary-card__actions") mustBe None
          }
        case cardActionValues :: Nil =>
          val cardLink: Element = titleWrapper
            .selectHead("div.govuk-summary-card__actions")
            .selectHead("a")

          checkpoint {
            cardLink.text mustBe cardActionValues.text
          }
          checkpoint {
            cardLink.attr("href") mustBe cardActionValues.href
          }
          checkpoint {
            cardLink.selectHead("span.govuk-visually-hidden").text mustBe cardActionValues.visuallyHidden
          }
        case cardActionsValues =>
          cardActionsValues.zip(1 to cardActionsValues.length) foreach { case (action, actionIndex) =>
            val cardLink: Element = titleWrapper
              .selectHead(".govuk-summary-card__actions")
              .selectHead(s".govuk-summary-card__action:nth-of-type($actionIndex)")
              .selectHead("a")

            checkpoint {
              cardLink.text mustBe action.text
            }
            checkpoint {
              cardLink.attr("href") mustBe action.href
            }
            checkpoint {
              cardLink.selectHead("span.govuk-visually-hidden").text mustBe action.visuallyHidden
            }
          }
      }

      val cardContent: Element = card.selectHead(".govuk-summary-card__content")

      checkpoint {
        cardContent.mustHaveSummaryList(".govuk-summary-list")(rows)
      }

      checkpoint.reportAll()
      Succeeded
    }
    //scalastyle:on

    def mustHaveSummaryList(selector: String)(rows: Seq[SummaryListRowValues]): Assertion = {
      val checkpoint: Checkpoint = new Checkpoint()

      val summaryList = element.selectHead(selector)

      rows.zip(1 to rows.length) foreach { case (rowData, rowIndex) =>
        val row = summaryList.selectHead(s".govuk-summary-list__row:nth-of-type($rowIndex)")

        checkpoint {
          row.selectHead("dt.govuk-summary-list__key").text mustBe rowData.key
        }

        checkpoint {
          row.selectHead("dd.govuk-summary-list__value").text mustBe rowData.value.getOrElse("")
        }

        rowData.actions match {
          case Nil =>
            checkpoint {
              row.selectOptionally("dd.govuk-summary-list__actions") mustBe None
            }
          case actionValues :: Nil =>
            val link = row.selectHead("dd.govuk-summary-list__actions").selectHead("a")

            checkpoint {
              link.attr("href") mustBe actionValues.href
            }
            checkpoint {
              link.text mustBe actionValues.text
            }
            checkpoint {
              link.selectHead("span.govuk-visually-hidden").text mustBe actionValues.visuallyHidden
            }
          case actionsValues =>
            actionsValues.zip(1 to actionsValues.length) foreach { case (actionValues, actionIndex) =>
              val link = row.selectHead("dd.govuk-summary-list__actions").selectHead(s"a:nth-of-type($actionIndex)")
              checkpoint {
                link.attr("href") mustBe actionValues.href
              }
              checkpoint {
                link.text mustBe actionValues.text
              }
              checkpoint {
                link.selectHead("span.govuk-visually-hidden").text mustBe actionValues.visuallyHidden
              }
            }
        }
      }

      checkpoint.reportAll()
      Succeeded
    }

    def mustNotHaveSummaryListRow(key: String): Assertion = {
      val summaryListRows = element.selectHead(".govuk-summary-list").select(".govuk-summary-list__row")
      summaryListRows.contains(key) mustBe false
    }

    //scalastyle:off
    def mustHaveTaskList(selector: String)(idPrefix: String, items: Seq[TaskListItemValues]): Assertion = {
      val checkpoint: Checkpoint = new Checkpoint()

      val taskList: Element = element.selectHead(selector)

      items.zip(1 to items.length) foreach { case (itemValues, itemIndex) =>
        val taskListItem: Element = taskList.selectHead(s"li.govuk-task-list__item:nth-of-type($itemIndex)")
        val taskListNameAndHint: Element = taskListItem.selectHead(".govuk-task-list__name-and-hint")
        val taskListStatus: Element = taskListItem.selectHead(".govuk-task-list__status")

        itemValues.link map { linkHref =>
          val taskListItemLink: Element = taskListNameAndHint.selectHead(".govuk-task-list__link")
          checkpoint {
            taskListItemLink.attr("href") mustBe linkHref
          }
          checkpoint {
            taskListItemLink.text mustBe itemValues.text
          }

          val expectedAriaDescribedBy: String = Seq(
            itemValues.hint.map(_ => s"$idPrefix-$itemIndex-hint"),
            Some(s"$idPrefix-$itemIndex-status")
          ).flatten.mkString(" ")

          checkpoint {
            taskListItemLink.attr("aria-describedby") mustBe expectedAriaDescribedBy
          }
        } getOrElse {
          checkpoint {
            taskListNameAndHint.selectHead("div > div").text mustBe itemValues.text
          }
        }

        itemValues.hint foreach { hint =>
          val taskListHint: Element = taskListNameAndHint.selectHead(".govuk-task-list__hint")

          checkpoint {
            taskListHint.text() mustBe hint
          }
          checkpoint {
            taskListHint.id mustBe s"$idPrefix-$itemIndex-hint"
          }
        }

        checkpoint {
          taskListStatus.id mustBe s"$idPrefix-$itemIndex-status"
        }

        itemValues.tagColor map { tagColour =>
          val tag = taskListStatus.selectHead("strong")
          checkpoint {
            tag.text mustBe itemValues.tagText
          }
          checkpoint {
            if (tagColour == "blue") tag.attr("class") mustBe "govuk-tag" else tag.attr("class") mustBe s"govuk-tag govuk-tag--$tagColour"
          }
        } getOrElse {
          checkpoint {
            taskListStatus.text mustBe itemValues.tagText
          }
        }

      }
      checkpoint.reportAll()
      Succeeded
    }

    //scalastyle:on
    def mustHaveRadioInput(selector: String)(name: String,
                                             legend: String,
                                             isHeading: Boolean,
                                             isLegendHidden: Boolean,
                                             headingClasses: Option[String] = None,
                                             hint: Option[String],
                                             errorMessage: Option[String],
                                             radioContents: Seq[RadioItem],
                                             isInline: Boolean = false): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()
      val radioFieldSet: Element = element.selectHead(selector)

      validateFieldSetLegend(radioFieldSet, legend, isHeading, isLegendHidden, headingClasses, checkpoint)

      hint.foreach { hint =>
        val radioFieldSetHint: Element = radioFieldSet.selectHead(".govuk-hint")
        checkpoint {
          radioFieldSet.attr("aria-describedby") must include(radioFieldSetHint.attr("id"))
        }
        checkpoint {
          radioFieldSetHint.text mustBe hint
        }
      }

      errorMessage.foreach { errorMessage =>
        val radioFieldSetError: Element = radioFieldSet.selectHead(".govuk-error-message")
        checkpoint {
          radioFieldSet.attr("aria-describedby") must include(radioFieldSetError.attr("id"))
        }
        checkpoint {
          radioFieldSetError.text must include(errorMessage)
        }
      }

      val radioField: Element = if (isInline) element.selectHead(".govuk-radios--inline") else element.selectHead(".govuk-radios")

      radioContents.zipWithIndex foreach { case (radioContent, index) =>
        if (radioContent.divider.isDefined) {
          validateRadioDivider(radioField, radioContent, index, checkpoint)
        } else {
          validateRadioItem(radioField, name, radioContent, index, checkpoint)
        }
      }
      checkpoint.reportAll()
      Succeeded
    }

    private def validateFieldSetLegend(radioFieldSet: Element,
                                       legend: String,
                                       isHeading: Boolean,
                                       isLegendHidden: Boolean,
                                       headingClasses: Option[String],
                                       checkpoint: Checkpoint): Unit = {
      val radioFieldSetLegend: Element = radioFieldSet.selectHead("legend")
      if (isHeading) {
        checkpoint {
          radioFieldSetLegend.getH1Element.text mustBe legend
        }
      } else {
        checkpoint {
          radioFieldSetLegend.text mustBe legend
        }
      }
      if (isLegendHidden) {
        checkpoint {
          radioFieldSetLegend.attr("class") must include("govuk-visually-hidden")
        }
      } else {
        checkpoint {
          radioFieldSetLegend.attr("class") mustNot include("govuk-visually-hidden")

          headingClasses map { classes =>
            radioFieldSetLegend.attr("class") must include(classes)
          }

        }
      }
    }

    private def validateRadioItem(radioField: Element, name: String, radioItem: RadioItem, index: Int, checkpoint: Checkpoint): Unit = {
      val radioItemElement: Element = radioField.child(index)
      val radioInput: Element = radioItemElement.selectHead("input")
      val radioLabel: Element = radioItemElement.selectHead("label")
      val radioInputId: String = if (index == 0) name else s"$name-${index + 1}"

      checkpoint {
        radioItemElement.className() mustBe "govuk-radios__item"
      }
      checkpoint {
        radioInput.attr("id") mustBe radioInputId
      }
      checkpoint {
        radioInput.attr("name") mustBe name
      }
      checkpoint {
        radioInput.attr("type") mustBe "radio"
      }
      checkpoint {
        radioInput.attr("value") mustBe radioItem.value.getOrElse("")
      }
      checkpoint {
        radioLabel.attr("for") mustBe radioInput.attr("id")
      }
      checkpoint {
        Text(radioLabel.text) mustBe radioItem.content
      }
      radioItem.hint.foreach { hint =>
        checkpoint {
          Text(radioItemElement.selectHead(".govuk-radios__hint").text) mustBe hint.content
        }
      }
    }

    private def validateRadioDivider(radioField: Element, radioDivider: RadioItem, index: Int, checkpoint: Checkpoint): Unit = {
      val dividerElement: Element = radioField.child(index)
      checkpoint {
        dividerElement.className() mustBe "govuk-radios__divider"
      }
      checkpoint {
        dividerElement.text() mustBe radioDivider.divider.get
      }
    }

    def mustHaveYesNoRadioInputs(selector: String)(name: String,
                                                   legend: String,
                                                   isHeading: Boolean,
                                                   isLegendHidden: Boolean,
                                                   hint: Option[String],
                                                   errorMessage: Option[String],
                                                   yesHintId: Option[String] = None,
                                                   yesHint: Option[Text] = None,
                                                   noHintId: Option[String] = None,
                                                   noHint: Option[Text] = None,
                                                   inline: Boolean = true,
                                                   yesText: Option[String] = None,
                                                   noText: Option[String] = None)
    : Assertion = {
      mustHaveRadioInput(selector
      )(name = name,
        legend = legend,
        isHeading = isHeading,
        isLegendHidden = isLegendHidden,
        hint = hint,
        errorMessage = errorMessage,
        radioContents = Seq(
          RadioItem(
            id = Some(name),
            content = Text(yesText.getOrElse(Yes.toMessageString)),
            value = Some(Yes.toString),
            hint = yesHint map { hint =>
              Hint(
                id = yesHintId,
                content = hint
              )
            }
          ),
          RadioItem(
            id = Some(s"$name-2"),
            content = Text(noText.getOrElse(No.toMessageString)),
            value = Some(No.toString),
            hint = noHint map { hint =>
              Hint(
                id = noHintId,
                content = hint
              )
            }
          )
        ),
        isInline = inline
      )
    }

    def mustHaveTable(tableHeads: List[String], tableRows: List[List[String]], maybeCaption: Option[String] = None, hiddenTableCaption: Boolean = true): Assertion = {
      val table: Element = element.selectHead("table")

      maybeCaption map {
        captionString =>
          val caption = table.selectHead("caption")
          caption.text() must be(captionString)
          if (hiddenTableCaption) {
            caption.attr("class") must be("govuk-table__caption govuk-visually-hidden")
          }
      }

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

    def mustHaveTextInput(selector: String)(name: String,
                                            label: String,
                                            isLabelHidden: Boolean,
                                            isPageHeading: Boolean,
                                            hint: Option[String] = None,
                                            error: Option[String] = None,
                                            autoComplete: Option[String] = None,
                                            spellcheck: Option[Boolean] = None,
                                            inputType: String = "text"): Assertion = {
      val checkpoint: Checkpoint = new Checkpoint
      val formGroup: Element = element.selectHead(selector)
      val textInput: Element = formGroup.selectHead(s"input[name=$name]")

      validateTextInputLabel(name, label, isPageHeading, isLabelHidden, checkpoint)

      checkpoint {
        textInput.attr("type") mustBe inputType
      }

      checkpoint {
        textInput.attr("name") mustBe name
      }

      autoComplete.fold(
        checkpoint {
          textInput.hasAttr("autocomplete") mustBe false
        }
      ) { value =>
        checkpoint {
          textInput.attr("autocomplete") mustBe value
        }
      }

      spellcheck.fold(
        checkpoint {
          textInput.hasAttr("spellcheck") mustBe false
        }
      ) { value =>
        checkpoint {
          textInput.attr("spellcheck") mustBe value.toString
        }
      }

      hint.fold(
        checkpoint {
          element.selectOptionally(s"#$name-hint") mustBe None
        }
      ) { hintText =>
        checkpoint {
          element.selectHead(s"#$name-hint").text mustBe hintText
        }
        checkpoint {
          textInput.attr("aria-describedby") must include(s"$name-hint")
        }
      }

      error.fold(
        checkpoint {
          element.selectOptionally(s"#$name-error") mustBe None
        }
      ) { errorMessage =>
        checkpoint {
          element.selectHead(s"#$name-error").text mustBe s"Error: $errorMessage"
        }
        checkpoint {
          textInput.attr("aria-describedby") must include(s"$name-error")
        }
      }

      checkpoint.reportAll()
      Succeeded
    }

    def validateTextInputLabel(name: String,
                               label: String,
                               isPageHeading: Boolean,
                               isLabelHidden: Boolean,
                               checkpoint: Checkpoint): Unit = {
      val textInputLabel: Element = element.selectHead(s"label[for=$name]")

      checkpoint {
        textInputLabel.text mustBe label
      }
      checkpoint {
        textInputLabel.attr("for") mustBe name
      }
      checkpoint {
        textInputLabel.className() must include("govuk-label")
      }

      if (isPageHeading) {
        checkpoint {
          textInputLabel.className() must include("govuk-label--l")
        }
      } else if (isLabelHidden) {
        checkpoint {
          textInputLabel.className() must include("govuk-visually-hidden")
        }
      } else {
        checkpoint {
          textInputLabel.className() must include("govuk-!-font-weight-bold")
        }
      }
    }

    def mustHaveDateInput(id: String, legend: String, exampleDate: String, isHeading: Boolean, isLegendHidden: Boolean, legendClass: Option[String] = None, errorMessage: Option[String] = None, dateInputsValues: Seq[DateInputFieldValues]): Assertion = {
      val checkpoint: Checkpoint = new Checkpoint()

      val dateInputField: Element = element.selectHead(s"#$id")

      val dateLegend: Element = element.selectHead(".govuk-fieldset__legend")
      if (isHeading) {
        checkpoint {
          dateLegend.getH1Element.text mustBe legend
        }
      } else {
        checkpoint {
          dateLegend.text mustBe legend
        }
        if (isLegendHidden) {
          checkpoint {
            dateLegend.attr("class") must include("govuk-visually-hidden")
          }
        } else {
          checkpoint {
            dateLegend.attr("class") must include(legendClass.getOrElse(""))
          }
        }
      }

      val hintText: Element = element.selectHead(s"#$id-hint.govuk-hint")
      checkpoint {
        hintText.text mustBe exampleDate
      }

      dateInputsValues zip (1 to dateInputsValues.length) foreach { case (dateInputValues, index) =>

        val item: Element = dateInputField.selectNth(".govuk-date-input__item", index)
        val label = item.selectHead("label")
        val input = item.selectHead("input")

        checkpoint {
          label.text mustBe dateInputValues.label
        }
        checkpoint {
          label.attr("for") mustBe input.id
        }
        checkpoint {
          input.id mustBe input.attr("name")
        }
        checkpoint {
          input.attr("type") mustBe "text"
        }
        checkpoint {
          input.attr("inputmode") mustBe "numeric"
        }

        dateInputValues.value foreach { value =>
          input.attr("value") mustBe value
        }

      }

      errorMessage foreach { message =>

        val fieldset: Element = element.getFieldset
        val errorMessage: Element = element.selectHead(".govuk-error-message")
        checkpoint {
          fieldset.attr("aria-describedby") must include("startDate-error")
        }
        checkpoint {
          errorMessage.selectHead("p").attr("id") mustBe "startDate-error"
        }
        checkpoint {
          errorMessage.text mustBe s"Error: $message"
        }
      }

      checkpoint.reportAll()
      Succeeded
    }

    def listErrorMessages(errors: List[String]): Assertion = {
      errors.zipWithIndex.map {
        case (error, index) => element.select(s"span.error-notification:nth-child(${index + 1})").text mustBe error
      } forall (_ == succeed) mustBe true
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

  implicit class DateRangeString(val updateDeadline: UpdateDeadline) {
    def toRangeString(dateFormatter: LocalDate => String, fromToFormat: String = "%s - %s"): String =
      fromToFormat.format(dateFormatter(updateDeadline.accountingPeriodModel.startDate.toLocalDate), dateFormatter(updateDeadline.accountingPeriodModel.endDate.toLocalDate))


  }

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
        selector.get(selector.size() match {
          case 1 => 0
          case _ => 1
        })
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
            if (useTextForValue) {
              radioButton.attr("value") mustBe radio.text
            } else {
              radioButton.attr("value") mustBe radio.name
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
            if (useTextForValue) {
              checkbox.attr("value") mustBe text
            } else {
              checkbox.attr("value") mustBe "true"
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
          val ele = eles
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
          labelField.text() mustBe label
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
        val ele = eles
        // ele.tagName mustBe "input"
        ele.attr("type") mustBe "hidden"
      }

    def mustHaveSubmitButton(text: String): Unit =
      s"$name must have the a submit button (Button) '$text'" in {
        val submitButtons = element.select("button")
        submitButtons.size mustBe 1
        submitButtons.text() mustBe text
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

    def mustHaveSignInButton(text: String, href: String): Unit =
      s"$name must have a link button '$text'" in {
        val signInButton = element.select("a.govuk-button")

        signInButton.size mustBe 1
        signInButton.text() mustBe text
        signInButton.attr("href") mustBe href
      }

    def mustHaveSignOutLinkGovUk(text: String, optOrigin: Option[String] = None): Unit = {
      val id = "sign-out-button"
      optOrigin match {
        case Some(origin) => mustHaveALink(id, text, SignOutController.signOut.url)
        case _ =>
          s"$name have a link with text '$text' pointed to 'Sign Out'" in {
            Option(element.getElementById(id)) match {
              case None => fail(s"Unable to locate $id")
              case Some(link) if !link.tagName().equals("a") => fail(s"The element with id=$id is not a link")
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
        element.getElementById("continue-button").text() mustBe text
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
            override val name: String = n
            override val element: Element = ele()
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

  object TestView {
    // n.b. page must be call-by-name otherwise it would be evaluated before the fake application could start
    def apply(name: String,
              title: String,
              heading: String,
              page: => Html,
              isAgent: Boolean = false,
              showSignOutInBanner: Boolean = true): TestView = new TestView(name, title, heading, page, showSignOutInBanner, isAgent)
  }
}

object ViewSpec {
  // these two constants are used for testing the views
  val testCall = Call("POST", "/test-url")
  val viewTestRequest = FakeRequest("POST", "/test-url")
}
