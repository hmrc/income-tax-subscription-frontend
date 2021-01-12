/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.MessageLookup
import assets.MessageLookup.SignUpCompleteIndividual._
import assets.MessageLookup.SignUpCompleteIndividual.whatHappensNow._
import controllers.SignOutController
import models.DateModel
import models.common.IncomeSourceModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.TestModels.{testSummaryData, testSummaryDataBusinessNextTaxYear}
import views.ViewSpecTrait
import utilities.UnitTestTrait


class SignUpCompleteViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val action: Call = ViewSpecTrait.testCall
  val incomeSourceBusinessNextTaxYear: IncomeSourceModel = IncomeSourceModel(true, false, false)
  val incomeSourceBusinessProperty: IncomeSourceModel = IncomeSourceModel(true, true, false)
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest
  val testEndYearOfCurrentTaxPeriod = 2021
  val testUpdatesBeforeQ1 = List[(String, String)]()
  val testUpdatesAfterQ1 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"), ("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ2 = List(("5 July 2020", "2020"))
  val testUpdatesAfterQ2 = List(("5 October 2020", "2020"), ("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ3 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"))
  val testUpdatesAfterQ3 = List(("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ4 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"), ("5 January 2021", "2021"))
  val testUpdatesAfterQ4 = List(("5 April 2021", "2021"))

  def page(incomeSource: IncomeSourceModel, taxQuarter: String): Html = views.html.individual.incometax.subscription.sign_up_complete(
    summary = incomeSource match {
      case IncomeSourceModel(true, false, false) => testSummaryDataBusinessNextTaxYear
      case _ => testSummaryData
    },
    endYearOfCurrentTaxPeriod = testEndYearOfCurrentTaxPeriod,
    updatesBefore = taxQuarter match {
      case "Q1" => testUpdatesBeforeQ1
      case "Q2" => testUpdatesBeforeQ2
      case "Q3" => testUpdatesBeforeQ3
      case "Q4" => testUpdatesBeforeQ4
      case "Next" => testUpdatesBeforeQ1
    },
    updatesAfter = taxQuarter match {
      case "Q1" => testUpdatesAfterQ1
      case "Q2" => testUpdatesAfterQ2
      case "Q3" => testUpdatesAfterQ3
      case "Q4" => testUpdatesAfterQ4
      case "Next" => testUpdatesBeforeQ1
    },
  )(request, implicitly, appConfig)

  def documentCurrentTaxYear(taxQuarter: String): Document = Jsoup.parse(page(incomeSourceBusinessProperty, taxQuarter).body)

  def documentNextTaxYear: Document = Jsoup.parse(page(incomeSourceBusinessNextTaxYear, "Next").body)

  val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"

  "The Sign Up Complete view" should {

    s"have the title '$title'" in {
      val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
      documentNextTaxYear.title() mustBe (title + serviceNameGovUk)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        documentNextTaxYear.select("#confirmation-heading").hasClass("govuk-panel--confirmation") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = documentNextTaxYear.select("H1")
        s"has the text '${heading}'" in {
          heading.text() mustBe MessageLookup.SignUpCompleteIndividual.heading
        }

        "has the class 'heading-large'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }
    }

    "have a 'What you need to do next' section for Next Tax Year" which {

      s"has the section heading '${whatHappensNow.heading}'" in {
        documentNextTaxYear.select("#whatHappensNow h2").text() mustBe whatHappensNow.heading
      }

      s"has a paragraph stating complete steps '$para1'" in {
        documentNextTaxYear.select("#whatHappensNow p").get(0).text() mustBe para1
      }

      s"has an initial numeric point '$number1'" in {
        documentNextTaxYear.select("#whatHappensNow ol li").get(0).text() mustBe number1
        documentNextTaxYear.select("#whatHappensNow ol li").get(0).select("a").attr("href") mustBe appConfig.softwareUrl
      }

      s"has a 3rd numeric point '$number4'" in {
        documentNextTaxYear.select("#whatHappensNow ol li").get(1).text() mustBe number4
      }

      s"has a bullet pointed list detailing update dates" in {
        documentNextTaxYear.select("#whatHappensNow ol ul li").get(0).text() mustBe nextTaxYearJulyUpdate
        documentNextTaxYear.select("#whatHappensNow ol ul li").get(1).text() mustBe nextTaxYearOcoberUpdate
        documentNextTaxYear.select("#whatHappensNow ol ul li").get(2).text() mustBe nextTaxYearJanuaryUpdate
        documentNextTaxYear.select("#whatHappensNow ol ul li").get(3).text() mustBe nextTaxYearAprilUpdate
      }

      s"has a 4th numeric point '${number5}'" in {
        documentNextTaxYear.select("#whatHappensNow ol li").get(6).text() mustBe number5
      }
    }

    "have a 'What you need to do next' section for Current Tax Year" which {

      s"has an initial numbered point: '$number1'" in {
        documentCurrentTaxYear("Q1").select("#whatHappensNow ol li").get(0).text() mustBe number1
        documentCurrentTaxYear("Q1").select("#whatHappensNow ol li").get(0).select("a").attr("href") mustBe appConfig.softwareUrl
      }

      "for Tax Quarter Q1" should {
        s"have a second numbered point: '$currentYaxYearQuarterlyUpdates'" in {
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol li").get(1).text() mustBe currentYaxYearQuarterlyUpdates
        }

        "have a bullet pointed list of update dates" in {
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol ul li").get(0).text() mustBe currentTaxYearJulyUpdate
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol ul li").get(1).text() mustBe currentTaxYearOctoberUpdate
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol ul li").get(2).text() mustBe currentTaxYearJanuaryUpdate
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol ul li").get(3).text() mustBe currentTaxYearAprilUpdate
        }
        s"have a third numbered point: '$currentTaxYearAnnualUpdates'" in {
          documentCurrentTaxYear("Q1").select("#whatHappensNow ol li").get(6).text() mustBe currentTaxYearAnnualUpdates
        }
      }

      "for Tax Quarter Q2, Q3 & Q4" should {
        s"have a second numbered point: $currentTaxYearPreviousUpdates" in {
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol li").get(1).text() mustBe currentTaxYearPreviousUpdates
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol li").get(1).text() mustBe currentTaxYearPreviousUpdates
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol li").get(1).text() mustBe currentTaxYearPreviousUpdates
        }

        "have a bullet pointed list of previous update dates for Q2" in {
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
        }
        "have a bullet pointed list of previous update dates for Q3" in {
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(2)").text() mustBe currentTaxYearOctoberUpdate
        }
        "have a bullet pointed list of previous update dates for Q4" in {
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(2)").text() mustBe currentTaxYearOctoberUpdate
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol ul:nth-child(3) li:nth-child(3)").text() mustBe currentTaxYearJanuaryUpdate
        }

        s"have a third numbered point: $currentYaxYearQuarterlyUpdates" in {
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol li").get(3).text() mustBe currentYaxYearQuarterlyUpdates
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol li").get(4).text() mustBe currentYaxYearQuarterlyUpdates
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol li").get(5).text() mustBe currentYaxYearQuarterlyUpdates
        }

        "have a bullet pointed list of quarterly update dates for Q2" in {
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(1)").text() mustBe currentTaxYearOctoberUpdate
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(2)").text() mustBe currentTaxYearJanuaryUpdate
          documentCurrentTaxYear("Q2").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(3)").text() mustBe currentTaxYearAprilUpdate
        }
        "have a bullet pointed list of quarterly update dates for Q3" in {
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(1)").text() mustBe currentTaxYearJanuaryUpdate
          documentCurrentTaxYear("Q3").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(2)").text() mustBe currentTaxYearAprilUpdate
        }
        "have a bullet pointed list of quarterly update dates for Q4" in {
          documentCurrentTaxYear("Q4").select("#whatHappensNow ol ul:nth-child(5) li:nth-child(1)").text() mustBe currentTaxYearAprilUpdate
        }

        s"has a paragraph stating Income Tax Estimate '$para1'" in {
          documentNextTaxYear.select("#whatHappensNow p").get(0).text() mustBe para1
        }

        s"has a paragraph stating Info delay '$para2'" in {
          documentNextTaxYear.select("#whatHappensNow p").get(1).text() mustBe para2
        }

        "have a sign out button" in {
          val actionSignOut = documentNextTaxYear.getElementById("sign-out-button")
          actionSignOut.attr("role") mustBe "button"
          actionSignOut.text() mustBe signOut
          actionSignOut.attr("href") mustBe SignOutController.signOut(request.path).url
        }
      }
    }
    }
}
