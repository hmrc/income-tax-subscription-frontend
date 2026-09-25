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

  def page(mandatedCurrentYear: Boolean, selectedTaxYearIsNext: Boolean, userNameMaybe: Option[String], preference: Option[Boolean], usingSoftwareStatus: Boolean, signedUpDate: LocalDate): Html =
    signUpConfirmation(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, testNino, preference, usingSoftwareStatus, signedUpDate)

  def document(mandatedCurrentYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               preference: Option[Boolean] = None,
               usingSoftwareStatus: Boolean,
               signedUpDate: LocalDate): Document = {
    Jsoup.parse(page(mandatedCurrentYear, selectedTaxYearIsNext, userNameMaybe, preference, usingSoftwareStatus, signedUpDate).body)
  }

  "SignUpConfirmation" must {
    "use the correct template" in new TemplateViewTest(
      view = page(true, true, None, None, true, LocalDate.now),
      title = SignUpConfirmationMessages.title,
      isAgent = false,
      hasBackLink = false
    )
  }

  "The sign up confirmation view" when {
    "the user has software and for current year. Content identical across all scenarios" should {
      def mainContent(preference: Option[Boolean] = None): Element = document(mandatedCurrentYear = false, selectedTaxYearIsNext = false, preference = preference, usingSoftwareStatus = true, signedUpDate = LocalDate.now()).mainContent

      "has a header panel" which {
        "contains the panel heading" in {
          mainContent().select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }
      }

      "have a print link" in {
        val link = mainContent().selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains first paragraph" in {
        mainContent().select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paragraphDescription(false)
      }

      "contains date field" in {
        mainContent().select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.dateField
      }

      "contains what you must do heading" in {
        mainContent().selectNth("h2", 1).text() mustBe SignUpConfirmationMessages.whatYouMustDoHeading
      }

      "contains a first paragraph" in {
        mainContent().select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.paraOne
      }

      "contains bullet list for next steps" which {
        def bulletList: Element = mainContent().selectNth("ul", 1)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.nextStepsOne
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.nextStepsTwo
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.nextStepsThree
        }
      }

      "have the correct third bullet point with text and link" in {
        val thirdBullet = mainContent().selectNth("ul", 1).selectNth("li", 3)
        val link = thirdBullet.selectHead("a")

        link.text mustBe SignUpConfirmationMessages.nextStepsThreeLink
        link.attr("href") mustBe appConfig.getAccountUrl
      }

      "contains a second paragraph" in {
        mainContent().select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains bullet list for dealines" which {
        def bulletList: Element = mainContent().selectNth("ul", 2)

        "has a first item" in {
          bulletList.selectNth("li", 1).text mustBe SignUpConfirmationMessages.deadline1
        }
        "has a second item" in {
          bulletList.selectNth("li", 2).text mustBe SignUpConfirmationMessages.deadline2
        }
        "has a third item" in {
          bulletList.selectNth("li", 3).text mustBe SignUpConfirmationMessages.deadline3
        }
        "has a fourth item" in {
          bulletList.selectNth("li", 4).text mustBe SignUpConfirmationMessages.deadline4
        }
      }

      "contains a mtd paragraph with a link" in {
        def usingMtdPara() = mainContent().selectNth("p", 6)

        val expectedText = s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"

        usingMtdPara().text() mustBe expectedText
        val link = usingMtdPara().select("a")
        link.text() mustBe SignUpConfirmationMessages.usingMtdLink
        link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
      }

      "contains survey link" which {
        "has a link for survey" in {
          mainContent().selectNth(".govuk-link", 4).text mustBe SignUpConfirmationMessages.surveyText
          mainContent().selectNth(".govuk-link", 4).attr("href") mustBe SignUpConfirmationMessages.surveyLink
          mainContent().select("p.govuk-body").last().text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val currentTaxYearStartYear: Int = AccountingPeriodUtil.getCurrentTaxStartYear
    val nextTaxYearStartYear: Int = AccountingPeriodUtil.getNextTaxStartYear

    val title = "Confirmation - Sign up complete"
    val whatYouMustDoHeading = "Your next steps"
    val panelHeading = "You’re signed up for Making Tax Digital for Income Tax"
    val panelUserDetails = s"$testName | $testNino"
    private val paraCurrent: String = {
      s"This means that from 6 April $currentTaxYearStartYear you need to send your quarterly updates and submit your tax return using your chosen software."
    }
    private val paraNext: String = {
      s"This means that from 6 April $nextTaxYearStartYear you need to send your quarterly updates and submit your tax return using your chosen software."
    }

    def paragraphDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.paraNext
    else
      SignUpConfirmationMessages.paraCurrent

    val printLink = "Print this page"

    val dateField: String = {
      val date = implicitDateFormatter.LongDate(LocalDate.now()).toLongDate
      s"Today’s date: $date"
    }

    val paraOne = "To make sure that you’re ready to use Making Tax Digital for Income Tax, you must now:"
    val nextStepsOne = "get software that works for your individual needs, if you have not already got this"
    val nextStepsTwo = "authorise your chosen software (this links your software) so that it works with Making Tax Digital for Income Tax"
    val nextStepsThree = "check that Making Tax Digital for Income Tax has been added to your HMRC online account (opens in new tab)"
    val nextStepsThreeLink = "your HMRC online account (opens in new tab)"

    val paraTwo = "You need to send your quarterly updates for each of your sole trader and property income sources. The deadlines are:"

    val deadline1 = "7 August"
    val deadline2 = "7 November"
    val deadline3 = "7 February"
    val deadline4 = "7 May"

    val usingMtdPara = "Read the"
    val usingMtdParaEnd = "– this explains how to do the required next steps listed above."
    val usingMtdLink = "Use Making Tax Digital for Income Tax guide (opens in new tab)"
    val usingMtdLinkHref = "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax"

    val surveyText = "What do you think of this service (opens in new tab)"
    val surveyTextEnd = " (takes 30 seconds)"
    val surveyLink = appConfig.feedbackFrontendRedirectUrl

  }

}