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

package views.agent.confirmation

import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.{AccountingPeriodUtil, ImplicitDateFormatterImpl, ViewSpec}
import views.html.agent.confirmation.SignUpConfirmation

import java.time.LocalDate

class SignUpConfirmationViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  implicit val implicitDateFormatter: ImplicitDateFormatterImpl = app.injector.instanceOf[ImplicitDateFormatterImpl]

  private val signUpConfirmation = app.injector.instanceOf[SignUpConfirmation]

  val testName = "Lisa Khan"
  val testNino = "QQ123456L"
  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")
  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  def page(mandatedCurrentYear: Boolean,
           mandatedNextYear: Boolean,
           selectedTaxYearIsNext: Boolean,
           userNameMaybe: Option[String],
           usingSoftwareStatus: Boolean,
           signedUpDate: LocalDate): Html =
    signUpConfirmation(mandatedCurrentYear,
      mandatedNextYear,
      selectedTaxYearIsNext,
      userNameMaybe,
      testNino,
      testAccountingPeriodModel,
      usingSoftwareStatus,
      signedUpDate)

  def document(eligibleNextYearOnly: Boolean,
               mandatedCurrentYear: Boolean,
               mandatedNextYear: Boolean,
               selectedTaxYearIsNext: Boolean,
               userNameMaybe: Option[String] = Some(testName),
               usingSoftwareStatus: Boolean,
               signedUpDate: LocalDate): Document =
    Jsoup.parse(page(mandatedCurrentYear, mandatedNextYear, selectedTaxYearIsNext, userNameMaybe, usingSoftwareStatus, signedUpDate).body)

  "SignUpConfirmation" must {
    "use the correct template" in new TemplateViewTest(
      view = page(true, true, true, None, true, LocalDate.now),
      title = SignUpConfirmationMessages.title,
      isAgent = true,
      hasBackLink = false
    )
  }

  "The sign up confirmation view" when {
    "the user has software and for current year. Content identical across all scenarios" should {
      def mainContent: Element = document(eligibleNextYearOnly = false,
        mandatedCurrentYear = false,
        mandatedNextYear = false,
        selectedTaxYearIsNext = false,
        usingSoftwareStatus = false,
        signedUpDate = LocalDate.now()).mainContent

      "have a header panel" which {
        "contains the panel heading" in {
          mainContent.select(".govuk-panel").select("h1").text() mustBe SignUpConfirmationMessages.panelHeading
        }
      }

      "have a print link" in {
        val link = mainContent.selectNth(".govuk-link", 1)
        link.text mustBe SignUpConfirmationMessages.printLink
        link.attr("data-module") mustBe "hmrc-print-link"
        link.attr("href") mustBe "#"
      }

      "contains first paragraph" in {
        mainContent.select(".govuk-body").select("p").get(1).text() mustBe SignUpConfirmationMessages.paragraphDescription(false)
      }

      "contains client details field" in {
        mainContent.select(".govuk-body").select("p").get(2).text() mustBe SignUpConfirmationMessages.userDetails
      }

      "contains date field" in {
        mainContent.select(".govuk-body").select("p").get(3).text() mustBe SignUpConfirmationMessages.dateField
      }

      "contains your next steps heading" in {
        mainContent.selectHead("h2").text() mustBe SignUpConfirmationMessages.whatToDoHeading
      }

      "contains second paragraph" in {
        mainContent.select(".govuk-body").select("p").get(4).text() mustBe SignUpConfirmationMessages.paraOne
      }

      "contains bullet list for next steps" which {
        def bulletList: Element = mainContent.selectNth("ul", 1)

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

      "have the correct second bullet point with text and link" in {
        val secondBullet = mainContent.selectNth("ul", 1).selectNth("li", 2)
        val link = secondBullet.selectHead("a")

        link.text mustBe SignUpConfirmationMessages.nextStepsTwoLink
        link.attr("href") mustBe appConfig.agentServicesAccountHomeUrl
      }

      "contains third paragraph" in {
        mainContent.select(".govuk-body").select("p").get(5).text() mustBe SignUpConfirmationMessages.paraTwo
      }

      "contains bullet list for dealines" which {
        def bulletList: Element = mainContent.selectNth("ul", 2)

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
        val usingMtdPara = mainContent.selectNth("p", 7)
        val expectedText =
          s"${SignUpConfirmationMessages.usingMtdPara} ${SignUpConfirmationMessages.usingMtdLink} ${SignUpConfirmationMessages.usingMtdParaEnd}"
        usingMtdPara.text() mustBe expectedText
        val link = usingMtdPara.select("a")
        link.text() mustBe SignUpConfirmationMessages.usingMtdLink
        link.attr("href") mustBe SignUpConfirmationMessages.usingMtdLinkHref
      }

      "contains survey link" which {
        "has a link for survey" in {
          mainContent.selectNth(".govuk-link", 4).text mustBe SignUpConfirmationMessages.surveyText
          mainContent.selectNth(".govuk-link", 4).attr("href") mustBe SignUpConfirmationMessages.surveyLink
          mainContent.select("p.govuk-body").last().text mustBe SignUpConfirmationMessages.surveyText + SignUpConfirmationMessages.surveyTextEnd
        }
      }
    }
  }

  private object SignUpConfirmationMessages {
    val title = "Confirmation - Sign up complete"
    val whatToDoHeading = "Your next steps"
    val panelHeading = "Your client is signed up for Making Tax Digital for Income Tax"
    val userDetails = s"Client details: $testName – $testNino"
    private val paraCurrent =
      s"This means that from ${startDate.day} April 2010 you need to send your client’s quarterly updates and submit their tax return using your chosen software."
    private val paraNext =
      s"This means that from ${startDate.day} April 2010 you need to send your client’s quarterly updates and submit their tax return using your chosen software."

    def paragraphDescription(yearIsNext: Boolean): String = if (yearIsNext)
      SignUpConfirmationMessages.paraCurrent
    else
      SignUpConfirmationMessages.paraNext

    val printLink = "Print this page"

    val dateField: String = {
      val date = implicitDateFormatter.LongDate(LocalDate.now()).toLongDate
      s"Today’s date: $date"
    }

    val nextYear = AccountingPeriodUtil.getNextTaxEndYear - 1

    val nextStepsHeading = "Your next steps"
    val paraOne = "To make sure that you and your client are ready to use Making Tax Digital for Income Tax, you must now:"
    val nextStepsOne = "get software that works for your client’s individual needs, if you have not already got this"
    val nextStepsTwo = "check your client authorisations have been added to your agent services account (opens in new tab)"
    val nextStepsTwoLink = "agent services account (opens in new tab)"
    val nextStepsThree = "authorise your chosen software (this links your software) so that it works with Making Tax Digital for Income Tax"

    val paraTwo = "You need to send your quarterly updates for each of your client’s sole trader and property income sources. The deadlines are:"
    val deadline1 = "7 August"
    val deadline2 = "7 November"
    val deadline3 = "7 February"
    val deadline4 = "7 May"

    val usingMtdPara = "Read"
    val usingMtdParaEnd = "to find out how to do the required next steps."
    val usingMtdLink = "use Making Tax Digital for Income Tax (opens in new tab)"
    val usingMtdLinkHref = "https://www.gov.uk/guidance/using-making-tax-digital-for-income-tax"

    val surveyText = "What do you think of this service (opens in new tab)"
    val surveyTextEnd = " (takes 30 seconds)"
    val surveyLink = appConfig.feedbackFrontendAgentRedirectUrl

    val signUpAnotherClient = "Sign up another client"
  }
}
