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

import models.common.AccountingPeriodModel
import models.{DateModel, UpdateDeadline}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatter, ImplicitDateFormatterImpl, ViewSpec}
import views.html.individual.confirmation.SignUpConfirmation

import java.time.LocalDate
import java.time.Month._
import java.time.format.DateTimeFormatter
import scala.util.Random

//scalastyle:off
class SignUpConfirmationViewSpec extends ViewSpec {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  import implicitDateFormatter._

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String], preference: Option[Boolean]): Html =
    signUpConfirmation(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, testNino, preference)

  def document(mandatedCurrentYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               preference: Option[Boolean] = None): Document = {
    Jsoup.parse(page(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, preference).body)
  }

  "The sign up confirmation view" when {
    "the user is voluntary and eligible for this year" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference).mainContent

      "has a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }
        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the Before you start section in first position" which {

        "contains a heading" in {
          mainContent().selectNth("h3", 1).text() contains SignUpConfirmationMessages.beforeYouStartSection.heading
        }

        "contains a paragraph one" in {
          mainContent().selectNth("p", 3).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph1
        }

        "contains a paragraph two" in {
          mainContent().selectNth("p", 4).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph2
        }

        "contains a paragraph three" in {
          mainContent().selectNth("p", 5).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph3
        }

        "contains a link" in {
          val link = mainContent().selectNth(".govuk-link", 2)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.beforeYouStartSection.findSoftwareLinkText
        }
      }

      "contains a When you start section in second position" which {

        "which has a heading" in {
          mainContent().selectNth("h3", 2).text() contains SignUpConfirmationMessages.whenYouStartSection.heading
        }

        "has a Send quarterly updates sub section" which {

          def quarterlySection: Element = mainContent().selectHead("ol").selectNth("li", 1)

          "contains a heading" in {
            quarterlySection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.heading
          }

          "contains quarterly updates initial paragraph" in {
            quarterlySection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.paraOne
          }

          "contains a bullet list of quarter types" which {
            def bulletList = quarterlySection.selectHead("ul")

            "has a first item" in {
              bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemOne
            }

            "has a second item" in {
              bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemTwo
            }
          }

          "contains a table" in {
            quarterlySection.mustHaveTable(
              tableHeads = List(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterlyUpdate, SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.deadline),
              tableRows = List(
                List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
              ),
              maybeCaption = Some(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.tableCaption),
              hiddenTableCaption = false
            )
          }

          "contains a warning message" in {
            quarterlySection.selectHead(".govuk-warning-text").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.warningMessage
          }

          "contains a link to find software which opens in a new tab" in {
            val link = quarterlySection.selectHead("a")
            link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#sending-quarterly-updates-using-compatible-software"
            link.attr("target") mustBe "_blank"
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.sectionLinkText
          }
        }

        "has Send an end of period statement sub section" which {
          def endOfPeriodSection: Element = mainContent().selectHead("ol > li:nth-of-type(2)")

          "contains a heading" in {
            endOfPeriodSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.heading
          }

          "contains end of period paragraph" in {
            endOfPeriodSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.thisYearParagraph
          }
        }

        "has Submit a final declaration sub section" which {
          def finalDeclarationSection: Element = mainContent().selectHead("ol > li:nth-of-type(3)")

          "contains a heading" in {
            finalDeclarationSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.heading
          }

          "contains final declaration paragraph" in {
            finalDeclarationSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.paragraph
          }

          "contains a bullet list of types" which {

            def finalDeclarationbulletList = finalDeclarationSection.selectHead("ul")

            "has a first item" in {
              finalDeclarationbulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemOne
            }

            "has a second item" in {
              finalDeclarationbulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemTwo
            }
          }

          "contains a link" in {
            val link = finalDeclarationSection.selectHead("a")
            link.attr("href") mustBe appConfig.onlineServiceAccountUrl
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.onlineServiceLinkText
          }
        }
      }

      "contains a Report previous tax year section in third position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 5).text() mustBe SignUpConfirmationMessages.reportPreviousTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 11).text() mustBe SignUpConfirmationMessages.reportPreviousTax.paragraphThisYear
        }
      }

      "contains a Pay you tax section in fourth position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 6).text() mustBe SignUpConfirmationMessages.payYourTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 12).text() mustBe SignUpConfirmationMessages.payYourTax.paraOne
        }

        "contains a bullet list of payment types" which {

          def bulletList = mainContent().selectNth("ul",3)

          "has a first item" in {
            bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.payYourTax.bulletOne
          }

          "has a second item" in {
            bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.payYourTax.bulletTwo
          }

          "has a third item" in {
            bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.payYourTax.bulletThree
          }

          "has a fourth item" in {
            bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.payYourTax.bulletFour
          }

          "has a fifth item" in {
            bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.payYourTax.bulletFive
          }
        }

        "contains a paragraph with link" in {
          val link = mainContent().selectNth("a", 5)
          link.attr("href") mustBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          link.text mustBe SignUpConfirmationMessages.payYourTax.linkText
          mainContent().selectNth("p",13).text() mustBe SignUpConfirmationMessages.payYourTax.paraTwo
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None) : Element = mainContent(preference).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
          preferenceSection(preference = Some(true)).selectNth("p", 2).text mustBe SignUpConfirmationMessages.onlinePreferenceParaTwo
        }

        "has a paper preference when their opt in preference was false " in {
          preferenceSection(preference = Some(false)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.paperPreferencePara
        }

      }

    }

    "the user is voluntary and eligible for next year only" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("href") mustBe "javascript:window.print()"
      }

      "contains what you you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the Before you start section in first position" which {

        "contains a heading" in {
          mainContent().selectNth("h3", 1).text() contains SignUpConfirmationMessages.beforeYouStartSection.heading
        }

        "contains a paragraph one" in {
          mainContent().selectNth("p", 3).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph1
        }

        "contains a paragraph two" in {
          mainContent().selectNth("p", 4).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph2
        }

        "contains a paragraph three" in {
          mainContent().selectNth("p", 5).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph3
        }

        "contains a link" in {
          val link = mainContent().selectNth(".govuk-link", 2)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.beforeYouStartSection.findSoftwareLinkText
        }
      }

      "contains a When you start section in second position" which {

        "which has a heading" in {
          mainContent().selectNth("h3", 2).text() contains SignUpConfirmationMessages.whenYouStartSection.heading
        }

        "has a Send quarterly updates sub section" which {

          def quarterlySection: Element = mainContent().selectHead("ol").selectNth("li", 1)

          "contains a heading" in {
            quarterlySection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.heading
          }

          "contains quarterly updates initial paragraph" in {
            quarterlySection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.paraOne
          }

          "contains a bullet list of quarter types" which {
            def bulletList = quarterlySection.selectHead("ul")

            "has a first item" in {
              bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemOne
            }

            "has a second item" in {
              bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemTwo
            }
          }

          "contains a table" in {
            quarterlySection.mustHaveTable(
              tableHeads = List(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterlyUpdate, SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.deadline),
              tableRows = List(
                List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
              ),
              maybeCaption = Some(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.tableCaption),
              hiddenTableCaption = false
            )
          }

          "contains a link to find software which opens in a new tab" in {
            val link = quarterlySection.selectHead("a")
            link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#sending-quarterly-updates-using-compatible-software"
            link.attr("target") mustBe "_blank"
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.sectionLinkText
          }
        }

        "has Send an end of period statement sub section" which {
          def endOfPeriodSection: Element = mainContent().selectHead("ol > li:nth-of-type(2)")

          "contains a heading" in {
            endOfPeriodSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.heading
          }

          "contains end of period paragraph" in {
            endOfPeriodSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.nextYearParagraph
          }
        }

        "has Submit a final declaration sub section" which {
          def finalDeclarationSection: Element = mainContent().selectHead("ol > li:nth-of-type(3)")

          "contains a heading" in {
            finalDeclarationSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.heading
          }

          "contains final declaration paragraph" in {
            finalDeclarationSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.paragraph
          }

          "contains a bullet list of types" which {

            def finalDeclarationbulletList = finalDeclarationSection.selectHead("ul")

            "has a first item" in {
              finalDeclarationbulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemOne
            }

            "has a second item" in {
              finalDeclarationbulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemTwo
            }
          }

          "contains a link" in {
            val link = finalDeclarationSection.selectHead("a")
            link.attr("href") mustBe appConfig.onlineServiceAccountUrl
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.onlineServiceLinkText
          }
        }
      }

      "contains a Report previous tax year section in third position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 5).text() mustBe SignUpConfirmationMessages.reportPreviousTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 11).text() mustBe SignUpConfirmationMessages.reportPreviousTax.paragraphNextYear
        }
      }

      "contains a Pay your tax section in fourth position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 6).text() mustBe SignUpConfirmationMessages.payYourTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 12).text() mustBe SignUpConfirmationMessages.payYourTax.paraOne
        }

        "contains a bullet list of payment types" which {

          def bulletList = mainContent().selectNth("ul",3)

          "has a first item" in {
            bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.payYourTax.bulletOne
          }

          "has a second item" in {
            bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.payYourTax.bulletTwo
          }

          "has a third item" in {
            bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.payYourTax.bulletThree
          }

          "has a fourth item" in {
            bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.payYourTax.bulletFour
          }

          "has a fifth item" in {
            bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.payYourTax.bulletFive
          }
        }

        "contains a paragraph with link" in {
          val link = mainContent().selectNth("a", 5)
          link.attr("href") mustBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          link.text mustBe SignUpConfirmationMessages.payYourTax.linkText
          mainContent().selectNth("p",13).text() mustBe SignUpConfirmationMessages.payYourTax.paraTwo
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None) : Element = mainContent(preference).selectNth("div", 7)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
          preferenceSection(preference = Some(true)).selectNth("p", 2).text mustBe SignUpConfirmationMessages.onlinePreferenceParaTwo
        }

        "has a paper preference when their opt in preference was false " in {
          preferenceSection(preference = Some(false)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.paperPreferencePara
        }

      }
    }

    "the user is mandated and eligible for next year only" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = true, preference = preference).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(true)
        }
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the Before you start section in first position" which {

        "contains a heading" in {
          mainContent().selectNth("h3", 1).text() contains SignUpConfirmationMessages.beforeYouStartSection.heading
        }

        "contains a paragraph one" in {
          mainContent().selectNth("p", 3).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph1
        }

        "contains a paragraph two" in {
          mainContent().selectNth("p", 4).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph2
        }

        "contains a paragraph three" in {
          mainContent().selectNth("p", 5).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph3
        }

        "contains a link" in {
          val link = mainContent().selectNth(".govuk-link", 2)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.beforeYouStartSection.findSoftwareLinkText
        }
      }

      "contains a When you start section in second position" which {

        "which has a heading" in {
          mainContent().selectNth("h3", 2).text() contains SignUpConfirmationMessages.whenYouStartSection.heading
        }

        "has a Send quarterly updates sub section" which {

          def quarterlySection: Element = mainContent().selectHead("ol").selectNth("li", 1)

          "contains a heading" in {
            quarterlySection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.heading
          }

          "contains quarterly updates initial paragraph" in {
            quarterlySection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.paraOne
          }

          "contains a bullet list of quarter types" which {
            def bulletList = quarterlySection.selectHead("ul")

            "has a first item" in {
              bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemOne
            }

            "has a second item" in {
              bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemTwo
            }
          }

          "contains a table" in {
            quarterlySection.mustHaveTable(
              tableHeads = List(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterlyUpdate, SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.deadline),
              tableRows = List(
                List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
              ),
              maybeCaption = Some(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.tableCaption),
              hiddenTableCaption = false
            )
          }

          "contains a link to find software which opens in a new tab" in {
            val link = quarterlySection.selectHead("a")
            link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#sending-quarterly-updates-using-compatible-software"
            link.attr("target") mustBe "_blank"
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.sectionLinkText
          }
        }

        "has Send an end of period statement sub section" which {
          def endOfPeriodSection: Element = mainContent().selectHead("ol > li:nth-of-type(2)")

          "contains a heading" in {
            endOfPeriodSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.heading
          }

          "contains end of period paragraph" in {
            endOfPeriodSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.nextYearParagraph
          }
        }

        "has Submit a final declaration sub section" which {
          def finalDeclarationSection: Element = mainContent().selectHead("ol > li:nth-of-type(3)")

          "contains a heading" in {
            finalDeclarationSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.heading
          }

          "contains final declaration paragraph" in {
            finalDeclarationSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.paragraph
          }

          "contains a bullet list of types" which {

            def finalDeclarationbulletList = finalDeclarationSection.selectHead("ul")

            "has a first item" in {
              finalDeclarationbulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemOne
            }

            "has a second item" in {
              finalDeclarationbulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemTwo
            }
          }

          "contains a link" in {
            val link = finalDeclarationSection.selectHead("a")
            link.attr("href") mustBe appConfig.onlineServiceAccountUrl
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.onlineServiceLinkText
          }
        }
      }

      "contains a Report previous tax year section in third position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 5).text() mustBe SignUpConfirmationMessages.reportPreviousTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 11).text() mustBe SignUpConfirmationMessages.reportPreviousTax.paragraphNextYear
        }
      }

      "contains a Pay you tax section in fourth position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 6).text() mustBe SignUpConfirmationMessages.payYourTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 12).text() mustBe SignUpConfirmationMessages.payYourTax.paraOne
        }

        "contains a bullet list of payment types" which {

          def bulletList = mainContent().selectNth("ul",3)

          "has a first item" in {
            bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.payYourTax.bulletOne
          }

          "has a second item" in {
            bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.payYourTax.bulletTwo
          }

          "has a third item" in {
            bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.payYourTax.bulletThree
          }

          "has a fourth item" in {
            bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.payYourTax.bulletFour
          }

          "has a fifth item" in {
            bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.payYourTax.bulletFive
          }
        }

        "contains a paragraph with link" in {
          val link = mainContent().selectNth("a", 5)
          link.attr("href") mustBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          link.text mustBe SignUpConfirmationMessages.payYourTax.linkText
          mainContent().selectNth("p",13).text() mustBe SignUpConfirmationMessages.payYourTax.paraTwo
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None) : Element = mainContent(preference).selectNth("div", 7)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
          preferenceSection(preference = Some(true)).selectNth("p", 2).text mustBe SignUpConfirmationMessages.onlinePreferenceParaTwo
        }

        "has a paper preference when their opt in preference was false " in {
          preferenceSection(preference = Some(false)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.paperPreferencePara
        }

      }
    }

    "the user is mandated and eligible for this year" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = true, selectedTaxYearIsNext = false, preference = preference).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }

        "contains the user name and nino" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(0)
            .text() mustBe SignUpConfirmationMessages.panelUserDetails
        }

        "contains the description" in {
          mainContent().select(".govuk-panel")
            .select(".govuk-panel__body")
            .select("p")
            .get(1)
            .text() mustBe SignUpConfirmationMessages.panelDescription(false)
        }
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains the Before you start section in first position" which {

        "contains a heading" in {
          mainContent().selectNth("h3", 1).text() contains SignUpConfirmationMessages.beforeYouStartSection.heading
        }

        "contains a paragraph one" in {
          mainContent().selectNth("p", 3).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph1
        }

        "contains a paragraph two" in {
          mainContent().selectNth("p", 4).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph2
        }

        "contains a paragraph three" in {
          mainContent().selectNth("p", 5).text() mustBe SignUpConfirmationMessages.beforeYouStartSection.paragraph3
        }

        "contains a link" in {
          val link = mainContent().selectNth(".govuk-link", 2)
          link.attr("href") mustBe "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
          link.text mustBe SignUpConfirmationMessages.beforeYouStartSection.findSoftwareLinkText
        }
      }

      "contains a When you start section in second position" which {

        "which has a heading" in {
          mainContent().selectNth("h3", 2).text() contains SignUpConfirmationMessages.whenYouStartSection.heading
        }

        "has a Send quarterly updates sub section" which {

          def quarterlySection: Element = mainContent().selectHead("ol").selectNth("li", 1)

          "contains a heading" in {
            quarterlySection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.heading
          }

          "contains quarterly updates initial paragraph" in {
            quarterlySection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.paraOne
          }

          "contains a bullet list of quarter types" which {
            def bulletList = quarterlySection.selectHead("ul")

            "has a first item" in {
              bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemOne
            }

            "has a second item" in {
              bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterTypesItemTwo
            }
          }

          "contains a table" in {
            quarterlySection.mustHaveTable(
              tableHeads = List(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.quarterlyUpdate, SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.deadline),
              tableRows = List(
                List(q1Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q1Update.deadline.toLongDateNoYear),
                List(q2Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q2Update.deadline.toLongDateNoYear),
                List(q3Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q3Update.deadline.toLongDateNoYear),
                List(q4Update.toRangeString(d => d.toLongDateNoYear, "%s to %s"), q4Update.deadline.toLongDateNoYear)
              ),
              maybeCaption = Some(SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.tableCaption),
              hiddenTableCaption = false
            )
          }

          "contains a warning message" in {
            quarterlySection.selectHead(".govuk-warning-text").text() contains SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.warningMessage
          }

          "contains a link to find software which opens in a new tab" in {
            val link = quarterlySection.selectHead("a")
            link.attr("href") mustBe "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax#sending-quarterly-updates-using-compatible-software"
            link.attr("target") mustBe "_blank"
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.quarterlyUpdates.sectionLinkText
          }
        }

        "has Send an end of period statement sub section" which {
          def endOfPeriodSection: Element = mainContent().selectHead("ol > li:nth-of-type(2)")

          "contains a heading" in {
            endOfPeriodSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.heading
          }

          "contains end of period paragraph" in {
            endOfPeriodSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.endOfPeriod.thisYearParagraph
          }
        }

        "has Submit a final declaration sub section" which {
          def finalDeclarationSection: Element = mainContent().selectHead("ol > li:nth-of-type(3)")

          "contains a heading" in {
            finalDeclarationSection.selectHead("h3").text() contains SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.heading
          }

          "contains final declaration paragraph" in {
            finalDeclarationSection.selectHead("p").text() mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.paragraph
          }

          "contains a bullet list of types" which {

            def finalDeclarationbulletList = finalDeclarationSection.selectHead("ul")

            "has a first item" in {
              finalDeclarationbulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemOne
            }

            "has a second item" in {
              finalDeclarationbulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.bulletItemTwo
            }
          }

          "contains a link" in {
            val link = finalDeclarationSection.selectHead("a")
            link.attr("href") mustBe appConfig.onlineServiceAccountUrl
            link.text mustBe SignUpConfirmationMessages.whenYouStartSection.finalDeclaration.onlineServiceLinkText
          }
        }
      }

      "contains a Report previous tax year section in third position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 5).text() mustBe SignUpConfirmationMessages.reportPreviousTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 11).text() mustBe SignUpConfirmationMessages.reportPreviousTax.paragraphThisYear
        }
      }

      "contains a Pay you tax section in fourth position" which {

        "has a heading" in {
          mainContent().selectNth("h3", 6).text() mustBe SignUpConfirmationMessages.payYourTax.heading
        }

        "has a paragraph" in {
          mainContent().selectNth("p", 12).text() mustBe SignUpConfirmationMessages.payYourTax.paraOne
        }

        "contains a bullet list of payment types" which {

          def bulletList = mainContent().selectNth("ul",3)

          "has a first item" in {
            bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.payYourTax.bulletOne
          }

          "has a second item" in {
            bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.payYourTax.bulletTwo
          }

          "has a third item" in {
            bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.payYourTax.bulletThree
          }

          "has a fourth item" in {
            bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.payYourTax.bulletFour
          }

          "has a fifth item" in {
            bulletList.selectNth("li", 5).text mustBe SignUpConfirmationMessages.payYourTax.bulletFive
          }
        }

        "contains a paragraph with link" in {
          val link = mainContent().selectNth("a", 5)
          link.attr("href") mustBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          link.text mustBe SignUpConfirmationMessages.payYourTax.linkText
          mainContent().selectNth("p",13).text() mustBe SignUpConfirmationMessages.payYourTax.paraTwo
        }
      }

      "contains a preference section" which {

        def preferenceSection(preference: Option[Boolean] = None) : Element = mainContent(preference).selectNth("div", 8)

        "has no retrieved preference content when no preference was provided to the view" in {
          preferenceSection().selectOptionalNth("p", 1) mustBe None
        }

        "has an online preference when their opt in preference was true" in {
          preferenceSection(preference = Some(true)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.onlinePreferenceParaOne
          preferenceSection(preference = Some(true)).selectNth("p", 2).text mustBe SignUpConfirmationMessages.onlinePreferenceParaTwo
        }

        "has a paper preference when their opt in preference was false " in {
          preferenceSection(preference = Some(false)).selectNth("p", 1).text mustBe SignUpConfirmationMessages.paperPreferencePara
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val whatYouMustDoHeading = "What you must do"
    val panelHeading = "Sign up complete"
    val panelUserDetails = s"$testName | $testNino"
    val panelDescriptionThis: String = {
      val yearStart = AccountingPeriodUtil.getCurrentTaxYear.startDate.year
      val yearEnd = AccountingPeriodUtil.getCurrentTaxYear.endDate.year
      s"is now signed up for Making Tax Digital for Income Tax for the current tax year (6 April $yearStart to 5 April $yearEnd)"
    }
    val panelDescriptionNext: String = {
      val yearStart = AccountingPeriodUtil.getNextTaxYear.startDate.year
      val yearEnd = AccountingPeriodUtil.getNextTaxYear.endDate.year
      s"is now signed up for Making Tax Digital for Income Tax for the next tax year (6 April $yearStart to 5 April $yearEnd)"
    }

    def panelDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.panelDescriptionNext
    else
      SignUpConfirmationMessages.panelDescriptionThis

    val printLink = "Print your confirmation"

    object beforeYouStartSection  {
      val heading = "Before you start"
      val paragraph1 = "To start using Making Tax Digital for Income Tax you must get compatible software. You should check if the software meets your business needs."
      val paragraph2 = "For example, if you want to update your income and expenses by calendar quarterly period dates you must choose software that supports this."
      val paragraph3 = "This must be done before you make your first update."
      val findSoftwareLinkText = "Find compatible software (opens in new tab)"
    }

    object whenYouStartSection {
      val heading = "When you start"

      object quarterlyUpdates {
        val heading = "Send quarterly updates"
        val paraOne = "You must send quarterly updates. The quarterly period dates are:"
        val quarterTypesItemOne = "calendar quarters (for example, 1 April to 30 June)"
        val quarterTypesItemTwo = "standard quarters (starts on the 6th date of each month)"
        val tableCaption = "Quarterly updates by the deadline"
        val sectionLinkText = "Learn more about quarterly updates (opens in new tab)"
        val quarterlyUpdate = "Quarterly update"
        val deadline = "Deadline"

        val warningMessage = "You must make updates for any quarters you’ve missed."
      }

      object endOfPeriod {
        val heading = "Send an end of period statement"
        val thisYearDate: String = AccountingPeriodUtil.getEndOfPeriodStatementDate(false).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        val nextYearDate: String = AccountingPeriodUtil.getEndOfPeriodStatementDate(true).format(DateTimeFormatter.ofPattern("D MMMM YYYY"))
        val thisYearParagraph = s"You must submit an end of period statement using your software by $thisYearDate."
        val nextYearParagraph = s"You must submit an end of period statement using your software by $nextYearDate."
      }

      object finalDeclaration {
        val heading = "Submit a final declaration"
        val paragraph = "Other income sources should be declared, such as income from employment, dividends or savings. You need to report this income using either your:"
        val bulletItemOne = "compatible software (if it has the functionality)"
        val bulletItemTwo = "HMRC online services account"
        val onlineServiceLinkText = "You can find more information in your HMRC online services account."
      }
    }

    object reportPreviousTax {
      val thisYear = AccountingPeriodUtil.getCurrentTaxEndYear
      val nextYear = AccountingPeriodUtil.getNextTaxEndYear
      val heading = "Report previous tax year"
      val paragraphThisYear = s"You must submit your Self Assessment tax return for the year ended 5 April $thisYear using your HMRC online services account as normal."
      val paragraphNextYear = s"You must submit your Self Assessment tax return for the year ended 5 April $nextYear using your HMRC online services account as normal."
    }

    object payYourTax {
      val heading = "Pay your tax"
      val paraOne = "There are many ways to pay your tax, including:"
      val bulletOne = "online banking"
      val bulletTwo = "telephone banking"
      val bulletThree = "debit card"
      val bulletFour = "corporate credit card"
      val bulletFive = "direct debit"
      val linkText = "how to pay your tax bill"
      val paraTwo = s"GOV.UK has more information on $linkText."
    }

    val onlinePreferenceParaOne = "You have chosen to get your tax letters online."
    val onlinePreferenceParaTwo = "You must verify your email address to confirm this. Select the link we sent by email to do this, if you have not already done so."
    val paperPreferencePara = "You have chosen to get your tax letters by post. You can change this at anytime in your HMRC online account."

    val printThisPage = "Print this page"
  }

  private val CURRENT_TAX_YEAR: Int = Random.between(1900, 2100)
  private val FIFTH: Int = 5
  private val SIXTH: Int = 6
  private lazy val q1Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, AUGUST, FIFTH))
  private lazy val q2Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JULY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, NOVEMBER, FIFTH))
  private lazy val q3Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, OCTOBER, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, FEBRUARY, FIFTH))
  private lazy val q4Update: UpdateDeadline = UpdateDeadline(
    AccountingPeriodModel(
      LocalDate.of(CURRENT_TAX_YEAR - 1, JANUARY, SIXTH),
      LocalDate.of(CURRENT_TAX_YEAR - 1, APRIL, FIFTH)
    ),
    LocalDate.of(CURRENT_TAX_YEAR - 1, MAY, FIFTH))

}