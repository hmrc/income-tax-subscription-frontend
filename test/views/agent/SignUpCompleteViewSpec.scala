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

import agent.assets.MessageLookup
import agent.assets.MessageLookup.SignUpComplete._
import models.{AccountingYear, Current, Next}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import services.AccountingPeriodService
import utilities.{CurrentDateProvider, UnitTestTrait}
import views.ViewSpecTrait
import views.html.agent.SignUpComplete

import java.time.LocalDate

class SignUpCompleteViewSpec extends UnitTestTrait with BeforeAndAfterEach{
  val mockCurrentDateProvider: CurrentDateProvider = mock[CurrentDateProvider]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCurrentDateProvider)
  }

  val action: Call = ViewSpecTrait.testCall

  val testClientName = "Test User"
  val testClientNino = "1234567890"

  private val taxYearEnd: Int = 2022
  private val q1 = LocalDate.of(2021, 4, 21)
  private val q2 = LocalDate.of(2021, 7, 21)
  private val q3 = LocalDate.of(2021, 10, 21)
  private val q4 = LocalDate.of(2022, 3, 21)

  def page(taxYear: Option[AccountingYear]): Html = {
    val signUpComplete: SignUpComplete = GuiceApplicationBuilder()
      .overrides(inject.bind[AccountingPeriodService].to(new AccountingPeriodService(mockCurrentDateProvider)))
      .build().injector.instanceOf[SignUpComplete]

    signUpComplete(
      taxYearSelection = taxYear,
      clientName = testClientName,
      clientNino = testClientNino,
      postAction = controllers.agent.routes.AddAnotherClientController.addAnother(),
      signOutAction = action
    )(FakeRequest(), implicitly, appConfig)
  }

  def documentNextTaxYear: Document = {
    when(mockCurrentDateProvider.getCurrentDate) thenReturn q4
    Jsoup.parse(page(Some(Next)).body)
  }

  def documentCurrentTaxYear(currentDate: LocalDate): Document = {
    when(mockCurrentDateProvider.getCurrentDate) thenReturn currentDate
    Jsoup.parse(page(Some(Current)).body)
  }

  "The Sign Up Complete view" should {
    s"have the title '$title'" in {
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      documentNextTaxYear.title() mustBe (SignUpComplete.title + serviceNameGovUk)
    }

    "have a sign-up-complete panel" which {
      "has a green background" in {
        documentNextTaxYear.select(".govuk-panel").hasClass("govuk-panel--confirmation") mustBe true
      }

      s"has a heading" which {
        lazy val heading = documentNextTaxYear.select("H1")

        s"has the correct text" in {
          heading.text() mustBe SignUpComplete.heading
        }

        "has the class 'heading-large'" in {
          heading.hasClass("govuk-panel__title") mustBe true
        }
      }

      "has paragraph 1" in {
        documentNextTaxYear.select(".govuk-panel__body p:nth-of-type(1)").text() mustBe SignUpComplete.panelParagraph1
      }

      "has paragraph 2" in {
        documentNextTaxYear.select(".govuk-panel__body p:nth-of-type(2)").text() mustBe SignUpComplete.panelParagraph2
      }
    }

    "have a 'What you need to do next' section for Next Tax Year" which {
      "has the section heading" in {
        documentNextTaxYear.selectFirst("h2").text() mustBe SignUpComplete.whatNowHeading
      }

      "has the 1st list item" in {
        documentNextTaxYear.select("ol > li:nth-of-type(1)").text() mustBe SignUpComplete.whatNowNumber1
        documentNextTaxYear.select("ol > li:nth-of-type(1)").select("a").attr("href") mustBe appConfig.softwareUrl
      }

      s"has a 2nd list item" which {
        "has paragraph1" in {
          documentNextTaxYear.select("ol.govuk-list > li:nth-of-type(2) > p:nth-of-type(1)").text mustBe SignUpComplete.NextYear.whatNowNumber2Para1
        }

        "have filling and deadline dates" in {
          val tableRows =
            documentNextTaxYear
              .select("ol.govuk-list > li:nth-of-type(2)")
              .select(".govuk-table")
              .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateOne(taxYearEnd.toString), SignUpComplete.deadlineDateOne(taxYearEnd.toString))
            assertTableRow(tableRows, rowIndex = 2, SignUpComplete.fillingDateTwo(taxYearEnd.toString), SignUpComplete.deadlineDateTwo(taxYearEnd.toString))
            assertTableRow(tableRows, rowIndex = 3, SignUpComplete.fillingDateThree((taxYearEnd).toString, (taxYearEnd + 1).toString), SignUpComplete.deadlineDateThree((taxYearEnd + 1).toString))
            assertTableRow(tableRows, rowIndex = 4, SignUpComplete.fillingDateFour((taxYearEnd + 1).toString), SignUpComplete.deadlineDateFour((taxYearEnd + 1).toString))
        }
      }

      "has a 3rd list item" in {
        documentNextTaxYear.select("ol.govuk-list > li:nth-of-type(3)").text mustBe SignUpComplete.NextYear.whatNowNumber3
      }

      "has a 4th list item" which {
        "has paragraph1" in {
          documentNextTaxYear.select("ol.govuk-list > li:nth-of-type(4)").text mustBe SignUpComplete.NextYear.whatNowNumber4
        }
      }
    }

    "have a 'What you need to do next' section for Current Tax Year" which {
      "has the section heading" in {
        documentCurrentTaxYear(q1).selectFirst("h2").text() mustBe SignUpComplete.whatNowHeading
      }

      s"has the 1st list item" in {
        val doc = documentCurrentTaxYear(q1)
        doc.select("ol > li:nth-of-type(1)").text() mustBe SignUpComplete.whatNowNumber1
        doc.select("ol > li:nth-of-type(1)").select("a").attr("href") mustBe appConfig.softwareUrl
      }

      "for Tax Quarter Q1" should {
        "have a 2nd list item" which {
          "has paragraph1" in {
            documentCurrentTaxYear(q1).select("ol.govuk-list > li:nth-of-type(2) > p:nth-of-type(1)").text mustBe SignUpComplete.CurrentYear.whatNowNumber3
          }

          "have filling and deadline dates" in {
            val tableRows =
              documentCurrentTaxYear(q1)
                .select("ol.govuk-list > li:nth-of-type(2)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateOne((taxYearEnd - 1).toString), SignUpComplete.deadlineDateOne((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 2, SignUpComplete.fillingDateTwo((taxYearEnd - 1).toString), SignUpComplete.deadlineDateTwo((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 3, SignUpComplete.fillingDateThree((taxYearEnd - 1).toString, taxYearEnd.toString), SignUpComplete.deadlineDateThree(taxYearEnd.toString))
            assertTableRow(tableRows, rowIndex = 4, SignUpComplete.fillingDateFour(taxYearEnd.toString), SignUpComplete.deadlineDateFour(taxYearEnd.toString))
          }
        }

        "has a 3rd list item" in {
          documentCurrentTaxYear(q1).select("ol.govuk-list > li:nth-of-type(3)").text mustBe SignUpComplete.CurrentYear.whatNowNumber4
        }

        "has a 4rd list item" in {
          documentCurrentTaxYear(q1).select("ol.govuk-list > li:nth-of-type(4)").text mustBe SignUpComplete.CurrentYear.whatNowNumber5
        }
      }

      "for Tax Quarter Q2, Q3 and Q4" should {
        val currentDate = q4

        "have a 2nd list item" which {
          "has paragraph1" in {
            documentCurrentTaxYear(currentDate).select("ol.govuk-list > li:nth-of-type(2) > p:nth-of-type(1)").text mustBe SignUpComplete.CurrentYear.whatNowNumber2Para1
          }

          "have filling and deadline dates for Q2" in {
            val tableRows =
              documentCurrentTaxYear(q2)
                .select("ol.govuk-list > li:nth-of-type(2)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateOne((taxYearEnd - 1).toString), SignUpComplete.deadlineDateOne((taxYearEnd - 1).toString))
          }

          "have filling and deadline dates for Q3" in {
            val tableRows =
              documentCurrentTaxYear(q3)
                .select("ol.govuk-list > li:nth-of-type(2)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateOne((taxYearEnd - 1).toString), SignUpComplete.deadlineDateOne((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 2, SignUpComplete.fillingDateTwo((taxYearEnd - 1).toString), SignUpComplete.deadlineDateTwo((taxYearEnd - 1).toString))
          }

          "have filling and deadline dates for Q4" in {
            val tableRows =
              documentCurrentTaxYear(q4)
                .select("ol.govuk-list > li:nth-of-type(2)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateOne((taxYearEnd - 1).toString), SignUpComplete.deadlineDateOne((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 2, SignUpComplete.fillingDateTwo((taxYearEnd - 1).toString), SignUpComplete.deadlineDateTwo((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 3, SignUpComplete.fillingDateThree((taxYearEnd - 1).toString, taxYearEnd.toString), SignUpComplete.deadlineDateThree(taxYearEnd.toString))
          }

          "has paragraph2" in {
            documentCurrentTaxYear(currentDate).select("ol.govuk-list > li:nth-of-type(2) > p:nth-of-type(2)").text mustBe SignUpComplete.CurrentYear.whatNowNumber2Para2
          }
        }

        "has a 3rd list item" which {
          "has paragraph1" in {
            documentCurrentTaxYear(currentDate).select("ol.govuk-list > li:nth-of-type(3) > p:nth-of-type(1)").text mustBe SignUpComplete.CurrentYear.whatNowNumber3
          }

          "have filling and deadline dates for Q2" in {
            val tableRows: Elements =
              documentCurrentTaxYear(q2)
                .select("ol.govuk-list > li:nth-of-type(3)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateTwo((taxYearEnd - 1).toString), SignUpComplete.deadlineDateTwo((taxYearEnd - 1).toString))
            assertTableRow(tableRows, rowIndex = 2, SignUpComplete.fillingDateThree((taxYearEnd - 1).toString, taxYearEnd.toString), SignUpComplete.deadlineDateThree(taxYearEnd.toString))
          }

          "have filling and deadline dates for Q3" in {
            val tableRows: Elements =
              documentCurrentTaxYear(q3)
                .select("ol.govuk-list > li:nth-of-type(3)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateThree((taxYearEnd - 1).toString, taxYearEnd.toString), SignUpComplete.deadlineDateThree(taxYearEnd.toString))
          }

          "have filling and deadline dates for Q4" in {
            val tableRows: Elements =
              documentCurrentTaxYear(q4)
                .select("ol.govuk-list > li:nth-of-type(3)")
                .select(".govuk-table")
                .select(".govuk-table__row")

            assertTableRow(tableRows, rowIndex = 0, SignUpComplete.updatesHeader, SignUpComplete.deadlineHeader, isHeader = true)
            assertTableRow(tableRows, rowIndex = 1, SignUpComplete.fillingDateFour(taxYearEnd.toString), SignUpComplete.deadlineDateFour(taxYearEnd.toString))
          }
        }
      }
    }

    s"has ${SignUpComplete.whatNowHeading}  paragraph 1" in {
      documentNextTaxYear.select(":not(ol) > p").get(4).text() mustBe SignUpComplete.whatNowHeadingParagraph1
    }

    s"has ${SignUpComplete.whatNowHeading} paragraph 2" in {
      documentNextTaxYear.select(":not(ol) > p").get(5).text() mustBe SignUpComplete.whatNowHeadingParagraph2
    }

    "have a add another client button" in {
      documentNextTaxYear.getElementsByClass("govuk-button").get(0).text() mustBe MessageLookup.Base.addAnother
    }

    "have a sign out link" in {
      documentNextTaxYear.getElementsByClass("govuk-link").get(0).text() mustBe MessageLookup.Base.signOut
    }
  }

  private def assertTableRow(tableRows: Elements, rowIndex: Int, expectedContentCell1: String, expectedContentCell2: String, isHeader: Boolean = false) = {
    val cellSelector = if (isHeader) ".govuk-table__header" else ".govuk-table__cell"
    val tableCells =
      tableRows
        .get(rowIndex)
        .select(cellSelector)

    tableCells.get(0).text() mustBe expectedContentCell1
    tableCells.get(1).text() mustBe expectedContentCell2
  }

  private object SignUpComplete {
    val title = "You have signed up Test User 1234567890 to use software to send Income Tax updates"
    val updatesHeader = "Quarterly update"
    val deadlineHeader = "Deadline"
    val heading = "Sign up complete"
    val panelParagraph1 = "Test User 1234567890 is now signed up for Making Tax Digital for Income Tax."
    val panelParagraph2 = "You can now use software to send Income Tax updates"

    val whatNowHeading = "What happens now"
    val whatNowHeadingParagraph1 = "After you’ve sent an update, your client will get an Income Tax year-to-date estimate. " +
      "They can see what they owe for the tax year after you’ve sent their final update."
    val whatNowHeadingParagraph2 = "It may take a few hours before new information is displayed in your agent service account."

    val whatNowNumber1LinkText = "find software that’s compatible (opens in new tab)"
    val whatNowNumber1 = s"If you have not already done so, $whatNowNumber1LinkText and allow it to interact with HMRC."

    object NextYear {
      val whatNowNumber2Para1 = "You will need to send quarterly updates using your software by:"
      val whatNowNumber3 = s"Submit your annual updates and declare for the tax year by 31 January ${taxYearEnd + 2}."
      val whatNowNumber4 = "We will not tell your client that you have signed them up for Making Tax Digital for Income Tax. " +
        "So you will need to inform them if they would like to use this service themselves."
    }

    object CurrentYear {
      val whatNowNumber2Para1 = "You need to add all income and expenses for the previous updates using your software for:"
      val whatNowNumber2Para2 = "There is no penalty if you or your client start making updates mid-way through the current tax year. " +
        "And, you can make as many updates as you wish, during each quarter."
      val whatNowNumber3 = "Send next quarterly updates using your software by:"
      val whatNowNumber4 = s"Submit your annual updates and declare for the tax year by 31 January ${taxYearEnd + 1}."
      val whatNowNumber5 = "We will not tell your client that you have signed them up for Making Tax Digital for Income Tax. " +
        "So you will need to inform them if they would like to use this service themselves."
    }

    def fillingDateOne(year: String): String = s"6 April $year - 5 July $year"

    def deadlineDateOne(year: String): String = s"5 August $year"

    def fillingDateTwo(year: String): String = s"6 July $year - 5 October $year"

    def deadlineDateTwo(year: String): String = s"5 November $year"

    def fillingDateThree(yearFrom: String, yearTo: String): String = s"6 October $yearFrom - 5 January $yearTo"

    def deadlineDateThree(year: String): String = s"5 February $year"

    def fillingDateFour(year: String): String = s"6 January $year - 5 April $year"

    def deadlineDateFour(year: String): String = s"5 May $year"
  }
}
