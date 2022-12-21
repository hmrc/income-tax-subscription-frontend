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

package views.agent

import models.{DateModel, UpdateDeadline}
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import scala.util.Random

class SignUpConfirmationViewSpec extends ViewSpec {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter.LongDate

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String]): Html =
    signUpConfirmation(selectedTaxYearIsNext, userNameMaybe, testNino, testAccountingPeriodModel)

  def document(selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String] = Some(testName)): Document =
    Jsoup.parse(page(selectedTaxYearIsNext, userNameMaybe).body)

  "The sign up confirmation view" when {

    for (yearIsNext <- Seq(true, false)) {
      val testMainContent = document(yearIsNext).mainContent
      s"nextYear flag is $yearIsNext" must {
        "have a section 1" which {
          "contains a heading" in {
            testMainContent.selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.section1heading
          }

          "contains the quarterly updates section" which {
            def quarterlyUpdates: Element = testMainContent.selectHead(".row")

            if (yearIsNext) {
              "contains a heading" in {

              }
              "contains Quarterly Updates initial paragraph" in {

              }
            } else {
              "contains a heading" in {
                quarterlyUpdates.selectNth("h3", 1).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesThisYearHeading
              }
              "contains Quarterly Updates initial paragraph" in {
                quarterlyUpdates.selectNth("p", 1).text() mustBe SignUpConfirmationMessages.quarterlyUpdatesThisYearParagraph
              }
              "contains a table" in {
                quarterlyUpdates.mustHaveTable(
                  tableHeads = List(SignUpConfirmationMessages.quarterlyUpdate, SignUpConfirmationMessages.deadline),
                  tableRows = List(
                    List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                    List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                    List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                    List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
                  ),
                  maybeCaption = Some(SignUpConfirmationMessages.quarterlyUpdatesTableCaption),
                  hiddenTableCaption = false
                )
              }
            }
          }
        }
        "have a header panel" which {
          "contains the panel heading" in {
            testMainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
          }
          "contains the user name and nino" in {
            testMainContent.select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(0)
              .text() mustBe SignUpConfirmationMessages.panelUserDetails
          }
          "contains the description" in {
            testMainContent.select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(1)
              .text() mustBe SignUpConfirmationMessages.panelDescription(yearIsNext)
          }
        }
        "have a check client details panel" which {
          "contains the check client details heading" in {
            testMainContent.select(".client-details").select("h2").text() mustBe SignUpConfirmationMessages.checkClientDetailsHeading
          }
          "contains the check client details text" in {
            testMainContent.select(".client-details")
              .select("p")
              .text() mustBe SignUpConfirmationMessages.checkClientDetailsText
          }
          "contains the check client details link" in {
            testMainContent.select(".client-details")
              .select("p")
              .select("a")
              .attr("href") mustBe appConfig.agentServicesAccountHomeUrl
          }
        }
        "have a button" in {
          testMainContent.selectHead(".govuk-button").text() mustBe SignUpConfirmationMessages.signUpAnotherClient
          testMainContent.selectHead(".govuk-button").attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val section1heading = "What you will have to do"
    val panelHeading = "Client sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    val panelDescriptionThis = s"is now signed up for Making Tax Digital for Income Tax for the current tax year (${startDate.day} April 2010 to ${endDate.day} April 2011)"
    val panelDescriptionNext = s"is now signed up for Making Tax Digital for Income Tax for the next tax year (${startDate.day} April 2010 to ${endDate.day} April 2011)"
    def panelDescription(yearIsNext: Boolean) = if (yearIsNext) SignUpConfirmationMessages.panelDescriptionNext else SignUpConfirmationMessages.panelDescriptionThis
    val signUpAnotherClient = "Sign up another client"
    val checkClientDetailsHeading = "Check your client’s account"
    val checkClientDetailsText = "Go to your agent service account to review or change the answers you have entered, and to get updates."
    val quarterlyUpdatesThisYearHeading = "1. Update us every quarter"
    val quarterlyUpdatesThisYearParagraph = "Your client will not face a penalty if you start making updates mid-way through the current tax year but you will need to make updates for the quarter’s you have missed."
    val quarterlyUpdate = "Quarterly update"
    val deadline = "Deadline"
    val quarterlyUpdatesTableCaption = "Quarterly updates by the deadline"
  }

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private val q1Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private val q2Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private val q3Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private val q4Update: UpdateDeadline = UpdateDeadline(LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH), LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH), LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))
}
