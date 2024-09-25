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
import models.{No, UpdateDeadline, Yes}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.Checkpoints.Checkpoint
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach, Succeeded}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.LocalDate
import scala.jdk.CollectionConverters._

trait ViewSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach with FeatureSwitching {

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val wrappedMessages: Messages = MessagesWrapper(Lang("en"), messagesApi)

  val testBackUrl = "/test-back-url"
  val testCall: Call = Call("POST", "/test-url")

  implicit val request: Request[_] = FakeRequest()

  class TemplateViewTest(view: Html,
                         title: String,
                         isAgent: Boolean = false,
                         backLink: Option[String] = None,
                         backLinkText: Option[String] = None,
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
      link.text mustBe backLinkText.getOrElse("Back")
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
      errorLink.text mustBe wrappedMessages(formError.message)
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

    def selectOptionalNth(selector: String, nth: Int): Option[Element] = {
      selectSeq(selector).lift(nth - 1)
    }

    def selectSeq(selector: String): Seq[Element] = {
      element.select(selector).asScala.toSeq
    }

    def selectNth(selector: String, nth: Int): Element = {
      selectSeq(selector).lift(nth - 1) match {
        case Some(element) => element
        case None => fail(s"Could not retrieve $selector number $nth")
      }
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

  case class TaskListItemValues(text: String, link: Option[String], hint: Option[String], tagText: String, tagColor: Option[String])

  case class SummaryListActionValues(href: String, text: String, visuallyHidden: String)

  case class SummaryListRowValues(key: String, value: Option[String], actions: Seq[SummaryListActionValues])

  case class DateInputFieldValues(label: String, value: Option[String])

  implicit class ElementTests(element: Element) {

    def mustHaveHeadingAndCaption(heading: String, caption: String, isSection: Boolean): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()

      checkpoint {
        element.selectHead("h1.govuk-heading-l").text mustBe heading
      }

      if (isSection) {
        checkpoint {
          element.selectHead("p.govuk-caption-l").text mustBe s"This section is $caption"
        }
        checkpoint {
          element.selectHead("p.govuk-caption-l").selectHead("span.govuk-visually-hidden").text mustBe "This section is"
        }
      } else {
        checkpoint {
          element.selectHead("span.govuk-caption-l").text mustBe caption
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
        titleWrapper.selectHead("h2").text mustBe title
      }

      cardActions match {
        case Nil =>
          checkpoint {
            titleWrapper.selectOptionally(".govuk-summary-card__actions") mustBe None
          }
        case cardActionValues :: Nil =>
          val cardLink: Element = titleWrapper
            .selectHead("div.govuk-summary-card__actions")
            .selectHead(".govuk-summary-card__action")
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
                                             hint: Option[String],
                                             errorMessage: Option[String],
                                             radioContents: Seq[RadioItem],
                                             isInline: Boolean = false): Assertion = {

      val checkpoint: Checkpoint = new Checkpoint()
      val radioFieldSet: Element = element.selectHead(selector)

      validateFieldSetLegend(radioFieldSet, legend, isHeading, isLegendHidden, checkpoint)

      hint.foreach{ hint =>
        val radioFieldSetHint: Element = radioFieldSet.selectHead(".govuk-hint")
        checkpoint {
          radioFieldSet.attr("aria-describedby") must include(radioFieldSetHint.attr("id"))
        }
        checkpoint {
          radioFieldSetHint.text mustBe hint
        }
      }

      errorMessage.foreach{ errorMessage =>
        val radioFieldSetError: Element = radioFieldSet.selectHead(".govuk-error-message")
        checkpoint {
          radioFieldSet.attr("aria-describedby") must include(radioFieldSetError.attr("id"))
        }
        checkpoint {
          radioFieldSetError.text must include (errorMessage)
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
        if (isLegendHidden) {
          checkpoint {
            radioFieldSetLegend.attr("class") must include("govuk-visually-hidden")
          }
        } else {
          checkpoint {
            radioFieldSetLegend.attr("class") mustNot include("govuk-visually-hidden")
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
                                                 errorMessage: Option[String])
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
              content = Text(Yes.toMessageString),
              value = Some(Yes.toString)
            ),
            RadioItem(
              id = Some(s"$name-2"),
              content = Text(No.toMessageString),
              value = Some(No.toString)
            )
          ),
        isInline = true
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

    def mustHaveDateInput(id: String, legend: String, exampleDate: String, isHeading: Boolean, isLegendHidden: Boolean, errorMessage: Option[String] = None, dateInputsValues: Seq[DateInputFieldValues]): Assertion = {
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
            dateLegend.attr("class") must include ("govuk-visually-hidden")
          }
        }
      }

      val hintText: Element = element.selectHead(s"#$id-hint.govuk-hint")
      checkpoint {
        hintText.text mustBe exampleDate
      }

      dateInputsValues zip(1 to dateInputsValues.length) foreach { case (dateInputValues, index) =>

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
          fieldset.attr("aria-describedby") must include ("startDate-error")
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

    def mustHaveTextField(name: String, label: String, hint: Option[String] = None): Assertion = {
      val eles = element.select(s"input[name=$name]")
      if (eles.isEmpty) fail(s"$name does not have an input field with name=$name\ncurrent list of inputs:\n[${element.select("input")}]")
      if (eles.size() > 1) fail(s"$name have multiple input fields with name=$name")
      val ele = eles.asScala.head
      ele.attr("type") mustBe "text"
      hint.map { hintValue =>
        element.selectHead(".govuk-hint").text mustBe hintValue
      }
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
}
