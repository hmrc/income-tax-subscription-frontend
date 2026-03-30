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

package views.individual.confirmation

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.confirmation.SignUpConfirmation

import java.time.LocalDate

class SignUpConfirmationViewSpec extends ViewSpec {

  implicit val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String], preference: Option[Boolean], usingSoftwareStatus: Boolean, signedUpDate: LocalDate, showHelp: Boolean): Html =
    signUpConfirmation(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, testNino, preference, usingSoftwareStatus, signedUpDate, showHelp)

  def document(mandatedCurrentYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               preference: Option[Boolean] = None,
               usingSoftwareStatus: Boolean,
               signedUpDate: LocalDate,
               showHelp: Boolean): Document = {
    Jsoup.parse(page(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, preference, usingSoftwareStatus, signedUpDate, showHelp).body)
  }

  "The sign up confirmation view" when {
    "the user has software and eligible for current year" should {
      def mainContent(preference: Option[Boolean] = None, showHelp: Boolean): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference, usingSoftwareStatus = true, signedUpDate = LocalDate.now(), showHelp).mainContent

      "has a header panel" which {
        "contains the panel heading" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
          }
        }

        "contains the description" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(0)
              .text() mustBe SignUpConfirmationMessages.panelDescription(false)
          }
        }
      }

      "have a print link" in {
        Seq(false, true).foreach { showHelp =>
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 1)
          link.text mustBe SignUpConfirmationMessages.printLink
          link.attr("data-module") mustBe "hmrc-print-link"
          link.attr("href") mustBe "#"
        }
      }

      "contains date field" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.dateField
        }
      }

      "contains what you must do heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
        }
      }

      "contains a first paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.paraOne
        }
      }

      "contains a second paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYear
        }
      }

      "contains bullets for cannot use HMRC reminders" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 1)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearBullet2
          }
        }
      }

      "contains a third paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearEnd
        }
      }

      "contains a mtd paragraph with a link" in {

        def usingMtdPara(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("p", 7)

        val expectedText = s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"

        Seq(false, true).foreach { showHelp =>
          usingMtdPara(showHelp).text() mustBe expectedText
          val link = usingMtdPara(showHelp).select("a")
          link.text() mustBe SignUpConfirmationMessages.usingMtdLink
          link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
        }
      }

      "contains a bullet list for mtd" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 2)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.usingMtdBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.usingMtdBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.usingMtdBullet3
          }
        }
      }

      "contains the quarterly updates section correctly" must {
        def quarterlyUpdatesSection(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("div", 7)

        "have the correct heading" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
          }
        }

        "have the correct intro paragraph" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("p").first().text() mustBe SignUpConfirmationMessages.quarterlyUpdatesPara1
          }
        }

        "have the correct table" which {
          "has the correct headers" in {
            Seq(false, true).foreach { showHelp =>
              val tableHeaders = quarterlyUpdatesSection(showHelp).select("th")
              tableHeaders.get(0).text() mustBe SignUpConfirmationMessages.updateDeadline
              tableHeaders.get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod
              tableHeaders.get(2).text() mustBe SignUpConfirmationMessages.standardPeriod
            }
          }

          "has the correct first row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(0).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline1
              tableRows.get(0).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod1
              tableRows.get(0).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod1
            }
          }

          "has the correct second row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(1).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline2
              tableRows.get(1).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod2
              tableRows.get(1).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod2
            }
          }

          "has the correct third row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(2).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline3
              tableRows.get(2).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod3
              tableRows.get(2).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod3
            }
          }

          "has the correct fourth row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(3).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline4
              tableRows.get(3).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod4
              tableRows.get(3).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod4
            }
          }
        }

        "have the correct read more paragraph" in {
          Seq(false, true).foreach { showHelp =>
            val readMorePara = quarterlyUpdatesSection(showHelp).select("p").last()
            readMorePara.text() must include(SignUpConfirmationMessages.quarterlyUpdatesPara2)
            readMorePara.select("a").attr("href") mustBe SignUpConfirmationMessages.quarterlyUpdatesPara2Link
          }
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None, showHelp: Boolean): Element = mainContent(preference, showHelp).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(showHelp = showHelp).selectOptionalNth("p", 1) mustBe None
          }
        }

        "has an online preference when their opt in preference was true" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(preference = Some(false), showHelp).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.postalPreferenceHeading
            preferenceSection(preference = Some(false), showHelp).selectNth("p", 1).text mustBe SignUpConfirmationMessages.postalPreferenceParaOne
          }
        }
      }

      "contains a CST contact section if showing helpo" which {
        "has a heading" in {
          mainContent(showHelp = true).selectNth("h2", 2).text mustBe SignUpConfirmationMessages.cstContactHeading
        }
        "has the contact details" in {
          mainContent(showHelp = true).selectNth("p.govuk-body", 10).text mustBe SignUpConfirmationMessages.cstContactPara
        }
        "has a link for call charges" in {
          mainContent(showHelp = true).selectNth(".govuk-link", 4).text mustBe SignUpConfirmationMessages.cstContactLinkText
          mainContent(showHelp = true).selectNth(".govuk-link", 4).attr("href") mustBe SignUpConfirmationMessages.cstContactLinkHref
        }
      }

      "contains survey link" which {
        "has a link for survey" in {
          Seq(false, true).foreach { showHelp =>
            val (l, p) = if (showHelp) (5, 12) else (4, 10)
            mainContent(showHelp = showHelp).selectNth(".govuk-link", l).text mustBe SignUpConfirmationMessages.surveyText
            mainContent(showHelp = showHelp).selectNth(".govuk-link", l).attr("href") mustBe SignUpConfirmationMessages.surveyLink
            mainContent(showHelp = showHelp).selectNth("p.govuk-body", p).text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
          }
        }
      }
    }

    "the user has software and for next year only" should {
      def mainContent(preference: Option[Boolean] = None, showHelp: Boolean): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference, usingSoftwareStatus = true, signedUpDate = LocalDate.now(), showHelp).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
          }
        }

        "contains the description" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(0)
              .text() mustBe SignUpConfirmationMessages.panelDescription(true)
          }
        }
      }

      "have a print link" in {
        Seq(false, true).foreach { showHelp =>
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 1)
          link.text mustBe SignUpConfirmationMessages.printLink
          link.attr("data-module") mustBe "hmrc-print-link"
          link.attr("href") mustBe "#"
        }
      }

      "contains date field" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.dateField
        }
      }

      "contains what you must do heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
        }
      }

      "contains a first paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.paraOne
        }
      }

      "contains a mtd paragraph with a link" in {

        def usingMtdPara(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("p", 5)

        val expectedText = s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"
        Seq(false, true).foreach { showHelp =>
          usingMtdPara(showHelp).text() mustBe expectedText
          val link = usingMtdPara(showHelp).select("a")
          link.text() mustBe SignUpConfirmationMessages.usingMtdLink
          link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
        }
      }

      "contains a bullet list for mtd" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 1)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.usingMtdBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.usingMtdBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.usingMtdBullet3
          }
        }
      }

      "contains the quarterly updates section correctly" must {
        def quarterlyUpdatesSection(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("div", 7)

        "have the correct heading" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
          }
        }

        "have the correct intro paragraph" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("p").first().text() mustBe SignUpConfirmationMessages.quarterlyUpdatesPara1
          }
        }

        "have the correct table" which {
          "has the correct headers" in {
            Seq(false, true).foreach { showHelp =>
              val tableHeaders = quarterlyUpdatesSection(showHelp).select("th")
              tableHeaders.get(0).text() mustBe SignUpConfirmationMessages.updateDeadline
              tableHeaders.get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod
              tableHeaders.get(2).text() mustBe SignUpConfirmationMessages.standardPeriod
            }
          }

          "has the correct first row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(0).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline1
              tableRows.get(0).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod1
              tableRows.get(0).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod1
            }
          }

          "has the correct second row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(1).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline2
              tableRows.get(1).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod2
              tableRows.get(1).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod2
            }
          }

          "has the correct third row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(2).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline3
              tableRows.get(2).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod3
              tableRows.get(2).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod3
            }
          }

          "has the correct fourth row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(3).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline4
              tableRows.get(3).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod4
              tableRows.get(3).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod4
            }
          }
        }

        "have the correct read more paragraph" in {
          Seq(false, true).foreach { showHelp =>
            val readMorePara = quarterlyUpdatesSection(showHelp).select("p").last()
            readMorePara.text() must include(SignUpConfirmationMessages.quarterlyUpdatesPara2)
            readMorePara.select("a").attr("href") mustBe SignUpConfirmationMessages.quarterlyUpdatesPara2Link
          }
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None, showHelp: Boolean): Element = mainContent(preference, showHelp).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(showHelp = showHelp).selectOptionalNth("p", 1) mustBe None
          }
        }

        "has an online preference when their opt in preference was true" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(preference = Some(false), showHelp).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.postalPreferenceHeading
            preferenceSection(preference = Some(false), showHelp).selectNth("p", 1).text mustBe SignUpConfirmationMessages.postalPreferenceParaOne
          }
        }
      }

      "does not contain a CST contact section" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectOptionalNth("h2", 3) mustBe None
          mainContent(showHelp = showHelp).selectOptionalNth("p.govuk-body", 9) mustBe None
        }
      }

      "contains survey link" which {
        "has a link for survey" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).selectNth(".govuk-link", 4).text mustBe SignUpConfirmationMessages.surveyText
            mainContent(showHelp = showHelp).selectNth(".govuk-link", 4).attr("href") mustBe SignUpConfirmationMessages.surveyLink
            mainContent(showHelp = showHelp).selectNth("p.govuk-body", 8).text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
          }
        }
      }
    }

    "the user has no software and for this year" should {
      def mainContent(preference: Option[Boolean] = None, showHelp: Boolean): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference, usingSoftwareStatus = false, signedUpDate = LocalDate.now(), showHelp).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
          }
        }

        "contains the description" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(0)
              .text() mustBe SignUpConfirmationMessages.panelDescription(false)
          }
        }
      }

      "have a print link" in {
        Seq(false, true).foreach { showHelp =>
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 1)
          link.text mustBe SignUpConfirmationMessages.printLink
          link.attr("data-module") mustBe "hmrc-print-link"
          link.attr("href") mustBe "#"
        }
      }

      "contains date field" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.dateField
        }
      }

      "contains what you must do heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
        }
      }

      "contains a first paragraph with a link" in {
        def firstPara(showHelp: Boolean) = mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(2)

        val expectedText = s"${SignUpConfirmationMessages.getSoftware} ${SignUpConfirmationMessages.getSoftwareLink}"
        Seq(false, true).foreach { showHelp =>
          firstPara(showHelp).text() mustBe expectedText
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 2)
          link.text mustBe SignUpConfirmationMessages.getSoftwareLink
          link.attr("href") mustBe SignUpConfirmationMessages.getSoftwareLinkHref
        }
      }

      "contains second paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.softwareUsagePara
        }
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 1)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.softwareUsageBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.softwareUsageBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.softwareUsageBullet3
          }
        }
      }

      "contains a third paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYear
        }
      }

      "contains bullets for cannot use HMRC reminders" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 2)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearBullet2
          }
        }
      }

      "contains a fourth paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.whatYouMustDoYesAndCurrentYearEnd
        }
      }

      "contains mtd heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.usingMtdHeading
        }
      }

      "contains a mtd paragraph with a link" in {

        def usingMtdPara(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("p", 8)

        val expectedText = s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"
        Seq(false, true).foreach { showHelp =>
          usingMtdPara(showHelp).text() mustBe expectedText
          val link = usingMtdPara(showHelp).select("a")
          link.text() mustBe SignUpConfirmationMessages.usingMtdLink
          link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
        }
      }

      "contains a bullet list for mtd" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 3)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.usingMtdBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.usingMtdBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.usingMtdBullet3
          }
        }
      }

      "contains the quarterly updates section correctly" must {
        def quarterlyUpdatesSection(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("div", 7)

        "have the correct heading" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
          }
        }

        "have the correct intro paragraph" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("p").first().text() mustBe SignUpConfirmationMessages.quarterlyUpdatesPara1
          }
        }

        "have the correct table" which {
          "has the correct headers" in {
            Seq(false, true).foreach { showHelp =>
              val tableHeaders = quarterlyUpdatesSection(showHelp).select("th")
              tableHeaders.get(0).text() mustBe SignUpConfirmationMessages.updateDeadline
              tableHeaders.get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod
              tableHeaders.get(2).text() mustBe SignUpConfirmationMessages.standardPeriod
            }
          }

          "has the correct first row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(0).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline1
              tableRows.get(0).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod1
              tableRows.get(0).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod1
            }
          }

          "has the correct second row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(1).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline2
              tableRows.get(1).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod2
              tableRows.get(1).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod2
            }
          }

          "has the correct third row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(2).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline3
              tableRows.get(2).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod3
              tableRows.get(2).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod3
            }
          }

          "has the correct fourth row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(3).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline4
              tableRows.get(3).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod4
              tableRows.get(3).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod4
            }
          }
        }

        "have the correct read more paragraph" in {
          Seq(false, true).foreach { showHelp =>
            val readMorePara = quarterlyUpdatesSection(showHelp).select("p").last()
            readMorePara.text() must include(SignUpConfirmationMessages.quarterlyUpdatesPara2)
            readMorePara.select("a").attr("href") mustBe SignUpConfirmationMessages.quarterlyUpdatesPara2Link
          }
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None, showHelp: Boolean): Element = mainContent(preference, showHelp).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(showHelp = showHelp).selectOptionalNth("p", 1) mustBe None
          }
        }

        "has an online preference when their opt in preference was true" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(preference = Some(false), showHelp).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.postalPreferenceHeading
            preferenceSection(preference = Some(false), showHelp).selectNth("p", 1).text mustBe SignUpConfirmationMessages.postalPreferenceParaOne
          }
        }
      }

      "contains a CST contact section if showing help" which {
        "has a heading" in {
          mainContent(showHelp = true).selectNth("h2", 3).text mustBe SignUpConfirmationMessages.cstContactHeading
        }

        "has the contact details" in {
          mainContent(showHelp = true).selectNth("p.govuk-body", 11).text mustBe SignUpConfirmationMessages.cstContactPara
        }

        "has a link for call charges" in {
          mainContent(showHelp = true).selectNth(".govuk-link", 5).text mustBe SignUpConfirmationMessages.cstContactLinkText
          mainContent(showHelp = true).selectNth(".govuk-link", 5).attr("href") mustBe SignUpConfirmationMessages.cstContactLinkHref
        }
      }

      "contains survey link" which {
        "has a link for survey" in {
          Seq(false, true).foreach { showHelp =>
            val (l, p) = if (showHelp) (6, 13) else (5, 11)
            mainContent(showHelp = showHelp).selectNth(".govuk-link", l).text mustBe SignUpConfirmationMessages.surveyText
            mainContent(showHelp = showHelp).selectNth(".govuk-link", l).attr("href") mustBe SignUpConfirmationMessages.surveyLink
            mainContent(showHelp = showHelp).selectNth("p.govuk-body", p).text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
          }
        }
      }
    }

    "the user has no software and for next year only" should {
      def mainContent(preference: Option[Boolean] = None, showHelp: Boolean): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference, usingSoftwareStatus = false, signedUpDate = LocalDate.now(), showHelp).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
          }
        }

        "contains the description" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).select(".govuk-panel")
              .select(".govuk-panel__body")
              .select("p")
              .get(0)
              .text() mustBe SignUpConfirmationMessages.panelDescription(true)
          }
        }
      }

      "have a print link" in {
        Seq(false, true).foreach { showHelp =>
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 1)
          link.text mustBe SignUpConfirmationMessages.printLink
          link.attr("data-module") mustBe "hmrc-print-link"
          link.attr("href") mustBe "#"
        }
      }

      "contains date field" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.dateField
        }
      }

      "contains what you must do heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
        }
      }

      "contains a first paragraph with a link" in {
        Seq(false, true).foreach { showHelp =>
          val firstPara = mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(2)
          val expectedText = s"${SignUpConfirmationMessages.getSoftware} ${SignUpConfirmationMessages.getSoftwareLink}"
          firstPara.text() mustBe expectedText
          val link = mainContent(showHelp = showHelp).selectNth(".govuk-link", 2)
          link.text mustBe SignUpConfirmationMessages.getSoftwareLink
          link.attr("href") mustBe SignUpConfirmationMessages.getSoftwareLinkHref
        }
      }

      "contains second paragraph" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.softwareUsagePara
        }
      }

      "contains a bullet list of what software will tell you to do" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 1)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.softwareUsageBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.softwareUsageBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.softwareUsageBullet3
          }
        }
      }

      "contains mtd heading" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectNth("h2", 2).text() mustBe SignUpConfirmationMessages.usingMtdHeading
        }
      }

      "contains a mtd paragraph with a link" in {
        Seq(false, true).foreach { showHelp =>
          val usingMtdPara = mainContent(showHelp = showHelp).selectNth("p", 6)
          val expectedText = s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"
          usingMtdPara.text() mustBe expectedText
          val link = usingMtdPara.select("a")
          link.text() mustBe SignUpConfirmationMessages.usingMtdLink
          link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
        }
      }

      "contains a bullet list for mtd" which {
        def bulletList(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("ul", 2)

        "has a first item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 1).text mustBe SignUpConfirmationMessages.usingMtdBullet1
          }
        }

        "has a second item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 2).text mustBe SignUpConfirmationMessages.usingMtdBullet2
          }
        }

        "has a third item" in {
          Seq(false, true).foreach { showHelp =>
            bulletList(showHelp).selectNth("li", 3).text mustBe SignUpConfirmationMessages.usingMtdBullet3
          }
        }
      }

      "contains the quarterly updates section correctly" must {
        def quarterlyUpdatesSection(showHelp: Boolean) = mainContent(showHelp = showHelp).selectNth("div", 7)

        "have the correct heading" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("h3").text() mustBe SignUpConfirmationMessages.quarterlyUpdatesHeading
          }
        }

        "have the correct intro paragraph" in {
          Seq(false, true).foreach { showHelp =>
            quarterlyUpdatesSection(showHelp).select("p").first().text() mustBe SignUpConfirmationMessages.quarterlyUpdatesPara1
          }
        }

        "have the correct table" which {
          "has the correct headers" in {
            Seq(false, true).foreach { showHelp =>
              val tableHeaders = quarterlyUpdatesSection(showHelp).select("th")
              tableHeaders.get(0).text() mustBe SignUpConfirmationMessages.updateDeadline
              tableHeaders.get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod
              tableHeaders.get(2).text() mustBe SignUpConfirmationMessages.standardPeriod
            }
          }

          "has the correct first row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(0).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline1
              tableRows.get(0).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod1
              tableRows.get(0).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod1
            }
          }

          "has the correct second row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(1).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline2
              tableRows.get(1).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod2
              tableRows.get(1).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod2
            }
          }

          "has the correct third row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(2).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline3
              tableRows.get(2).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod3
              tableRows.get(2).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod3
            }
          }

          "has the correct fourth row" in {
            Seq(false, true).foreach { showHelp =>
              val tableRows = quarterlyUpdatesSection(showHelp).select("tbody tr")
              tableRows.get(3).select("td").get(0).text() mustBe SignUpConfirmationMessages.deadline4
              tableRows.get(3).select("td").get(1).text() mustBe SignUpConfirmationMessages.calendarPeriod4
              tableRows.get(3).select("td").get(2).text() mustBe SignUpConfirmationMessages.standardPeriod4
            }
          }
        }

        "have the correct read more paragraph" in {
          Seq(false, true).foreach { showHelp =>
            val readMorePara = quarterlyUpdatesSection(showHelp).select("p").last()
            readMorePara.text() must include(SignUpConfirmationMessages.quarterlyUpdatesPara2)
            readMorePara.select("a").attr("href") mustBe SignUpConfirmationMessages.quarterlyUpdatesPara2Link
          }
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None, showHelp: Boolean): Element = mainContent(preference, showHelp).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(showHelp = showHelp).selectOptionalNth("p", 1) mustBe None
          }
        }

        "has an online preference when their opt in preference was true" in {
          Seq(false, true).foreach { showHelp =>
            preferenceSection(preference = Some(false), showHelp).selectNth("h2", 1).text mustBe SignUpConfirmationMessages.postalPreferenceHeading
            preferenceSection(preference = Some(false), showHelp).selectNth("p", 1).text mustBe SignUpConfirmationMessages.postalPreferenceParaOne
          }
        }
      }

      "does not contain a CST contact section" in {
        Seq(false, true).foreach { showHelp =>
          mainContent(showHelp = showHelp).selectOptionalNth("h2", 3) mustBe None
          mainContent(showHelp = showHelp).selectOptionalNth("p.govuk-body", 10) mustBe None
        }
      }

      "contains survey link" which {
        "has a link for survey" in {
          Seq(false, true).foreach { showHelp =>
            mainContent(showHelp = showHelp).selectNth(".govuk-link", 5).text mustBe SignUpConfirmationMessages.surveyText
            mainContent(showHelp = showHelp).selectNth(".govuk-link", 5).attr("href") mustBe SignUpConfirmationMessages.surveyLink
            mainContent(showHelp = showHelp).selectNth("p.govuk-body", 9).text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
          }
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val currentTaxYearStartYear: Int = AccountingPeriodUtil.getCurrentTaxStartYear
    val currentTaxYearEndYear: Int = AccountingPeriodUtil.getCurrentTaxEndYear
    val nextTaxYearStartYear: Int = AccountingPeriodUtil.getNextTaxStartYear
    val nextTaxYearEndYear: Int = AccountingPeriodUtil.getNextTaxEndYear

    val whatYouMustDoHeading = "What happens next"
    val panelHeading = "Sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    private val panelDescriptionThis: String = {
      s"You’re signed up for Making Tax Digital for Income Tax from (6 April $currentTaxYearStartYear to 5 April $currentTaxYearEndYear) onwards"
    }
    private val panelDescriptionNext: String = {
      s"You’re signed up for Making Tax Digital for Income Tax from (6 April $nextTaxYearStartYear to 5 April $nextTaxYearEndYear) onwards"
    }

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val printLink = "Print or save this page"

    val paraOne = s"You must submit your Self Assessment tax return using software that works with Making Tax Digital for Income Tax."

    val dateField: String = {
      val date = implicitDateFormatter.LongDate(LocalDate.now()).toLongDate
      s"Date: $date"
    }

    val whatYouMustDoYesAndCurrentYear = "You cannot use your HMRC online services to submit your other income sources for:"
    val whatYouMustDoYesAndCurrentYearBullet1 = s"the remainder of the $currentTaxYearStartYear to $currentTaxYearEndYear tax year"
    val whatYouMustDoYesAndCurrentYearBullet2 = s"the upcoming $nextTaxYearStartYear to $nextTaxYearEndYear tax year"
    val whatYouMustDoYesAndCurrentYearEnd = s"But you must submit your Self Assessment tax returns for the tax years up to 5 April $currentTaxYearStartYear as normal."

    val whatYouMustDoNoAndCurrentYear = s"You must find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkTextNoAndCurrentYear = "software that works with Making Tax Digital for Income Tax (opens in new tab)"

    val whatYouMustDoYesAndNextYear = s"From 6 April $nextTaxYearStartYear, you must use your software that works with Making Tax Digital for Income Tax."
    val whatYouMustDoNoAndNextYear = s"From 6 April $nextTaxYearStartYear, you must find and use software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val linkTextNoAndNextYear = "software that works with Making Tax Digital for Income Tax (opens in new tab)"

    val paraTwo = "Your chosen software will tell you what else you need to do, including:"
    val bullet1 = "how to authorise and connect the software to the Government Gateway user ID you use for your Self Assessment"
    val bullet2 = "how to keep digital records"
    val bullet3 = "when and how to send quarterly updates"
    val bullet4NoThisYear = "if you need to send any missed or backdated updates for the current tax year - and how to send them"
    val bullet5 = "when and how to make your tax return after the end of the tax year"
    val paraThree = "And you will need to pay the tax you owe."

    val cstContactHeading = "Get help with Making Tax Digital for Income Tax"
    val cstContactPara = "Telephone: 03003 229 619 Monday to Friday, 8am to 6pm (except public holidays)"
    val cstContactLinkText = "Find out about call charges (opens in new tab)"
    val cstContactLinkHref = "https://www.gov.uk/call-charges"

    val getSoftware = "You must get"
    val getSoftwareLink = "software that works with Making Tax Digital for Income Tax (opens in new tab)"
    val getSoftwareLinkHref = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

    val softwareUsagePara = "You must then use the software to:"
    val softwareUsageBullet1 = "create, store and correct digital records of your self-employment and property income and expenses"
    val softwareUsageBullet2 = "send your quarterly updates to HMRC"
    val softwareUsageBullet3 = "submit your tax return and pay tax due by 31 January the following year"


    val usingMtdHeading = "Using Making Tax Digital for Income Tax"
    val usingMtdPara = "Read"
    val usingMtdParaEnd = "to find out more information about:"
    val usingMtdLink = "use Making Tax Digital for Income Tax (opens in new tab)"
    val usingMtdLinkHref = "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax"
    val usingMtdBullet1 = "what to expect after you sign up"
    val usingMtdBullet2 = "the different steps you will need to take during the tax year"
    val usingMtdBullet3 = "help and support"

    val quarterlyUpdatesHeading = "Sending your quarterly updates"
    val quarterlyUpdatesPara1 = "You need to send your quarterly updates for each of your sole trader and property income sources by:"
    val quarterlyUpdatesPara2 = "You can read more about quarterly updates"
    val quarterlyUpdatesPara2Link = "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"

    val updateDeadline = "Update deadline"
    val deadline1 = "7 August"
    val deadline2 = "7 November"
    val deadline3 = "7 February"
    val deadline4 = "7 May"

    val calendarPeriod = "Calendar period"
    val calendarPeriod1 = "1 April to 30 June"
    val calendarPeriod2 = "1 April to 30 September"
    val calendarPeriod3 = "1 April to 31 December"
    val calendarPeriod4 = "1 April to 31 March"

    val standardPeriod = "Standard period"
    val standardPeriod1 = "6 April to 5 July"
    val standardPeriod2 = "6 April to 5 October"
    val standardPeriod3 = "6 April to 5 January"
    val standardPeriod4 = "6 April to 5 April"

    val surveyText = "What did you think of this service (opens in new tab)"
    val surveyTextEnd = " (takes 30 seconds)"
    val surveyLink = appConfig.feedbackFrontendRedirectUrl

    val postalPreferenceHeading = "Deadline reminders"
    val postalPreferenceParaOne = "To receive deadline reminders, you need to opt in for online communications. You can update your communication preferences at any time in your online tax account settings."
  }

}