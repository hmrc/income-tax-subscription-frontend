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

package views.individual.incometax.subscription

import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.{ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.incometax.subscription.SignUpComplete

import java.time.LocalDate
import java.time.Month._

class SignUpCompleteViewSpec extends ViewSpec {

  val mockAccountingPeriodService: AccountingPeriodService = mock[AccountingPeriodService]
  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountingPeriodService)
  }

  val CURRENT_TAX_YEAR: Int = 2021
  val FIFTH: Int = 5
  val SIXTH: Int = 6

  val q1Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  val q2Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  val q3Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  val q4Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))

  val signUpComplete: SignUpComplete = GuiceApplicationBuilder()
    .overrides(inject.bind[AccountingPeriodService].to(mockAccountingPeriodService))
    .build().injector.instanceOf[SignUpComplete]

  def page(selectedTaxYear: AccountingYear): Html = signUpComplete(
    taxYearSelection = Some(selectedTaxYear),
    postAction = testCall
  )

  def document(selectedTaxYear: AccountingYear): Document = Jsoup.parse(page(selectedTaxYear).body)

  object SignUpCompleteMessages {
    val heading = "You have signed up to use software to send Income Tax updates"
    val whatNowHeading = "What happens now"

    val findSoftwareLinkText = "find software that’s compatible (opens in new tab)"
    val findSoftwareText = s"If you have not already done so, $findSoftwareLinkText and allow it to interact with HMRC."

    val fileQuarterly = "You need to file quarterly updates using your software:"
    val sendUpdatesBy = "You will need to send quarterly updates using your software by:"
    val sendNextUpdatesBy = "Send next quarterly updates using your software by:"
    val noPenaltyMidYear = "There is no penalty if you start making updates mid-way through the current tax year."

    val quarterlyUpdate = "Quarterly update"
    val deadline = "Deadline"
    val quarterlyTableCaption = "Submit quarterly updates by the deadline"

    def submitAnnualBy(year: String): String = s"Submit your annual updates and declare for the tax year by 31 January $year."

    val btaLinkText = "Business Tax account (opens in new tab)"
    val para1 = s"After you have sent an update you will get an year-to-date Income Tax estimate. You can view your estimates and submission dates in your software or your $btaLinkText."
    val para2 = "It may take a few hours before new information is displayed."

    val finishAndSignOut = "Finish and sign out"
  }

  "The sign up complete view" must {

    "be using the correct template details" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      new TemplateViewTest(
        view = page(Next),
        title = SignUpCompleteMessages.heading,
      )
    }

    "have a confirmation banner heading" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      document(Next).mainContent.selectHead(".govuk-panel").selectHead("h1").text mustBe SignUpCompleteMessages.heading
    }

    "have a what now heading" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      document(Next).mainContent.selectHead("h2").text mustBe SignUpCompleteMessages.whatNowHeading
    }

    "display the correct details for the filing dates" when {
      "the user signed up for the next tax year" in {
        when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
          .thenReturn(List(q1Update, q2Update, q3Update, q4Update))
        when(mockAccountingPeriodService.currentTaxYear) thenReturn CURRENT_TAX_YEAR

        val orderedList: Element = document(Next).mainContent.selectHead("ol")

        val point1: Element = orderedList.selectNth("li", 1)
        val point1Link: Element = point1.selectHead("a")
        point1.text mustBe SignUpCompleteMessages.findSoftwareText
        point1Link.attr("href") mustBe appConfig.softwareUrl
        point1Link.text mustBe SignUpCompleteMessages.findSoftwareLinkText

        val point2: Element = orderedList.selectNth("li", 2)
        point2.selectNth("p", 1).text mustBe SignUpCompleteMessages.sendUpdatesBy
        point2.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q1Update.toRangeString(d => d.toLongDate), q1Update.deadline.toLongDate),
            List(q2Update.toRangeString(d => d.toLongDate), q2Update.deadline.toLongDate),
            List(q3Update.toRangeString(d => d.toLongDate), q3Update.deadline.toLongDate),
            List(q4Update.toRangeString(d => d.toLongDate), q4Update.deadline.toLongDate)
          )
        )

        val point3: Element = orderedList.selectNth("li", 3)
        point3.text mustBe SignUpCompleteMessages.submitAnnualBy((CURRENT_TAX_YEAR + 2).toString)
      }
      "the user signed up for the current tax year in Q1" in {
        when(mockAccountingPeriodService.getCurrentYearUpdateDates)
          .thenReturn(UpdateDeadlineDates(previous = List(), next = List(q1Update, q2Update, q3Update, q4Update)))
        when(mockAccountingPeriodService.currentTaxYear) thenReturn CURRENT_TAX_YEAR

        val orderedList: Element = document(Current).mainContent.selectHead("ol")

        val point1: Element = orderedList.selectNth("li", 1)
        val point1Link: Element = point1.selectHead("a")
        point1.text mustBe SignUpCompleteMessages.findSoftwareText
        point1Link.attr("href") mustBe appConfig.softwareUrl
        point1Link.text mustBe SignUpCompleteMessages.findSoftwareLinkText

        val point2: Element = orderedList.selectNth("li", 2)
        point2.selectNth("p", 1).text mustBe SignUpCompleteMessages.sendNextUpdatesBy
        point2.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q1Update.toRangeString(d => d.toLongDate), q1Update.deadline.toLongDate),
            List(q2Update.toRangeString(d => d.toLongDate), q2Update.deadline.toLongDate),
            List(q3Update.toRangeString(d => d.toLongDate), q3Update.deadline.toLongDate),
            List(q4Update.toRangeString(d => d.toLongDate), q4Update.deadline.toLongDate)
          )
        )

        val point3: Element = orderedList.selectNth("li", 3)
        point3.text mustBe SignUpCompleteMessages.submitAnnualBy((CURRENT_TAX_YEAR + 1).toString)
      }
      "the user signed up for the current tax year in Q2" in {
        when(mockAccountingPeriodService.getCurrentYearUpdateDates)
          .thenReturn(UpdateDeadlineDates(previous = List(q1Update), next = List(q2Update, q3Update, q4Update)))
        when(mockAccountingPeriodService.currentTaxYear) thenReturn CURRENT_TAX_YEAR

        val orderedList: Element = document(Current).mainContent.selectHead("ol")

        val point1: Element = orderedList.selectNth("li", 1)
        val point1Link: Element = point1.selectHead("a")
        point1.text mustBe SignUpCompleteMessages.findSoftwareText
        point1Link.attr("href") mustBe appConfig.softwareUrl
        point1Link.text mustBe SignUpCompleteMessages.findSoftwareLinkText

        val point2: Element = orderedList.selectNth("li", 2)
        point2.selectNth("p", 1).text mustBe SignUpCompleteMessages.fileQuarterly
        point2.selectNth("p", 2).text mustBe SignUpCompleteMessages.noPenaltyMidYear

        point2.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q1Update.toRangeString(d => d.toLongDate), q1Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point3: Element = orderedList.selectNth("li", 3)
        point3.selectHead("p").text mustBe SignUpCompleteMessages.sendNextUpdatesBy
        point3.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q2Update.toRangeString(d => d.toLongDate), q2Update.deadline.toLongDate),
            List(q3Update.toRangeString(d => d.toLongDate), q3Update.deadline.toLongDate),
            List(q4Update.toRangeString(d => d.toLongDate), q4Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point4: Element = orderedList.selectNth("li", 4)
        point4.text mustBe SignUpCompleteMessages.submitAnnualBy((CURRENT_TAX_YEAR + 1).toString)
      }
      "the user signed up for the current tax year in Q3" in {
        when(mockAccountingPeriodService.getCurrentYearUpdateDates)
          .thenReturn(UpdateDeadlineDates(previous = List(q1Update, q2Update), next = List(q3Update, q4Update)))
        when(mockAccountingPeriodService.currentTaxYear) thenReturn CURRENT_TAX_YEAR

        val orderedList: Element = document(Current).mainContent.selectHead("ol")

        val point1: Element = orderedList.selectNth("li", 1)
        val point1Link: Element = point1.selectHead("a")
        point1.text mustBe SignUpCompleteMessages.findSoftwareText
        point1Link.attr("href") mustBe appConfig.softwareUrl
        point1Link.text mustBe SignUpCompleteMessages.findSoftwareLinkText

        val point2: Element = orderedList.selectNth("li", 2)
        point2.selectNth("p", 1).text mustBe SignUpCompleteMessages.fileQuarterly
        point2.selectNth("p", 2).text mustBe SignUpCompleteMessages.noPenaltyMidYear
        point2.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q1Update.toRangeString(d => d.toLongDate), q1Update.deadline.toLongDate),
            List(q2Update.toRangeString(d => d.toLongDate), q2Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point3: Element = orderedList.selectNth("li", 3)
        point3.selectHead("p").text mustBe SignUpCompleteMessages.sendNextUpdatesBy
        point3.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q3Update.toRangeString(d => d.toLongDate), q3Update.deadline.toLongDate),
            List(q4Update.toRangeString(d => d.toLongDate), q4Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point4: Element = orderedList.selectNth("li", 4)
        point4.text mustBe SignUpCompleteMessages.submitAnnualBy((CURRENT_TAX_YEAR + 1).toString)
      }
      "the user signed up for the current tax year in Q4" in {
        when(mockAccountingPeriodService.getCurrentYearUpdateDates)
          .thenReturn(UpdateDeadlineDates(previous = List(q1Update, q2Update, q3Update), next = List(q4Update)))
        when(mockAccountingPeriodService.currentTaxYear) thenReturn CURRENT_TAX_YEAR

        val orderedList: Element = document(Current).mainContent.selectHead("ol")

        val point1: Element = orderedList.selectNth("li", 1)
        val point1Link: Element = point1.selectHead("a")
        point1.text mustBe SignUpCompleteMessages.findSoftwareText
        point1Link.attr("href") mustBe appConfig.softwareUrl
        point1Link.text mustBe SignUpCompleteMessages.findSoftwareLinkText

        val point2: Element = orderedList.selectNth("li", 2)
        point2.selectNth("p", 1).text mustBe SignUpCompleteMessages.fileQuarterly
        point2.selectNth("p", 2).text mustBe SignUpCompleteMessages.noPenaltyMidYear
        point2.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q1Update.toRangeString(d => d.toLongDate), q1Update.deadline.toLongDate),
            List(q2Update.toRangeString(d => d.toLongDate), q2Update.deadline.toLongDate),
            List(q3Update.toRangeString(d => d.toLongDate), q3Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point3: Element = orderedList.selectNth("li", 3)
        point3.selectHead("p").text mustBe SignUpCompleteMessages.sendNextUpdatesBy
        point3.mustHaveTable(
          tableHeads = List(SignUpCompleteMessages.quarterlyUpdate, SignUpCompleteMessages.deadline),
          tableRows = List(
            List(q4Update.toRangeString(d => d.toLongDate), q4Update.deadline.toLongDate)
          ),
          maybeCaption = Some(SignUpCompleteMessages.quarterlyTableCaption)
        )

        val point4: Element = orderedList.selectNth("li", 4)
        point4.text mustBe SignUpCompleteMessages.submitAnnualBy((CURRENT_TAX_YEAR + 1).toString)
      }
    }

    "have a description of where to view updates and estimates" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      document(Next).mainContent.selectNth("div.govuk-grid-column-two-thirds > p", 1).text mustBe SignUpCompleteMessages.para1
    }

    "have a description of how long it might take to update" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      document(Next).mainContent.selectNth("div.govuk-grid-column-two-thirds > p", 2).text mustBe SignUpCompleteMessages.para2
    }

    "have a finish and sign out button" in {
      when(mockAccountingPeriodService.getAllUpdateAndDeadlineDates(ArgumentMatchers.eq(Next)))
        .thenReturn(List(q1Update, q2Update, q3Update, q4Update))

      val actionSignOut = document(Next).mainContent.selectHead(".govuk-button")
      actionSignOut.text() mustBe SignUpCompleteMessages.finishAndSignOut
    }

  }

}
