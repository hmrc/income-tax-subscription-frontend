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
import views.html.individual.incometax.subscription.SignUpComplete


class SignUpCompleteViewSpec extends ViewSpecTrait {

  val signUpComplete: SignUpComplete = app.injector.instanceOf[SignUpComplete]

  val submissionDateValue: DateModel = DateModel("1", "1", "2016")
  val action: Call = ViewSpecTrait.testCall
  val incomeSourceBusinessNextTaxYear: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)
  val incomeSourceBusinessProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest
  val testEndYearOfCurrentTaxPeriod = 2021
  val testUpdatesBeforeQ1: List[(String, String)] = List[(String, String)]()
  val testUpdatesAfterQ1 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"), ("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ2 = List(("5 July 2020", "2020"))
  val testUpdatesAfterQ2 = List(("5 October 2020", "2020"), ("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ3 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"))
  val testUpdatesAfterQ3 = List(("5 January 2021", "2021"), ("5 April 2021", "2021"))
  val testUpdatesBeforeQ4 = List(("5 July 2020", "2020"), ("5 October 2020", "2020"), ("5 January 2021", "2021"))
  val testUpdatesAfterQ4 = List(("5 April 2021", "2021"))

  def page(incomeSource: IncomeSourceModel, taxQuarter: String): Html = signUpComplete(
    summary = incomeSource match {
      case IncomeSourceModel(true, false, false) => testSummaryDataBusinessNextTaxYear
      case _ => testSummaryData
    },
    postAction = controllers.individual.subscription.routes.ConfirmationController.submit(),
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
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      documentNextTaxYear.title() mustBe (title + serviceNameGovUk)
    }

    "have a successful transaction confirmation banner" which {

      "has a turquoise background" in {
        documentNextTaxYear.select("#comfirmation-panel").hasClass("govuk-panel govuk-panel--confirmation govuk-!-margin-bottom-8") mustBe true
      }

      s"has a heading (H1)" in {

        val heading = documentNextTaxYear.select("H1")
        heading.text() mustBe MessageLookup.SignUpCompleteIndividual.heading
        heading.hasClass("govuk-panel__title") mustBe true
      }
    }

    "have a sign out link on the top banner" in {
      val actionSignOut = documentNextTaxYear.getElementsByClass("hmrc-sign-out-nav__link")
      actionSignOut.text() mustBe signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut.url
    }

    "have a 'What you need to do next' section for Next Tax Year" which {

      s"has the section heading '${whatHappensNow.heading}'" in {
        documentNextTaxYear.select("#whatHappensNow h2").text() mustBe whatHappensNow.heading
      }

      s"has a paragraph stating complete steps '$para1'" in {
        documentNextTaxYear.select("#whatHappensNow p").get(0).text() mustBe para1
      }

      s"has an initial numeric point '$findSoftware'" in {
        documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(1)").text() mustBe findSoftware
        documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(1)").select("a").attr("href") mustBe appConfig.softwareUrl
      }

      s"has a 2nd numeric point '$sendQuarterlyBy'" which {
        "has some info about updates" in {
          documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(2) > span").text mustBe sendQuarterlyBy
        }
        "has bullet pointed list detailing update dates" in {
          documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(1)").text() mustBe nextTaxYearJulyUpdate
          documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(2)").text() mustBe nextTaxYearOcoberUpdate
          documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(3)").text() mustBe nextTaxYearJanuaryUpdate
          documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(4)").text() mustBe nextTaxYearAprilUpdate
        }
      }

      s"has a 3rd numeric point '$submitAnnualAndDeclare'" in {
        documentNextTaxYear.select("#whatHappensNow > ol > li:nth-of-type(3)").text() mustBe submitAnnualAndDeclare
      }
    }

    "have a 'What you need to do next' section for Current Tax Year" which {

      s"has an initial numbered point: '$findSoftware'" in {
        documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(1)").text() mustBe findSoftware
        documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(1)").select("a").attr("href") mustBe appConfig.softwareUrl
      }

      "for Tax Quarter Q1" should {
        s"have a second numbered point: '$currentYaxYearQuarterlyUpdates'" which {
          "has info about updates" in {
            documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(2) > span").text() mustBe currentYaxYearQuarterlyUpdates
          }
          "has a bullet pointed list of update dates" in {
            documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(1)").text() mustBe currentTaxYearJulyUpdate
            documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(2)").text() mustBe currentTaxYearOctoberUpdate
            documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(3)").text() mustBe currentTaxYearJanuaryUpdate
            documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-of-type(4)").text() mustBe currentTaxYearAprilUpdate
          }
        }
        s"have a third numbered point: '$currentTaxYearAnnualUpdates'" in {
          documentCurrentTaxYear("Q1").select("#whatHappensNow > ol > li:nth-of-type(3)").text() mustBe currentTaxYearAnnualUpdates
        }
      }

      "for Tax Quarter Q2, Q3 & Q4" should {
        s"have a second numbered point: $currentTaxYearPreviousUpdates" which {
          "has info on previous updates" in {
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(2) > span").text() mustBe currentTaxYearPreviousUpdates
          }
          "has a bullet pointed list of previous updates for Q2" in {
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
          }
          "has a bullet pointed list of previous updates for Q3" in {
            documentCurrentTaxYear("Q3").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
            documentCurrentTaxYear("Q3").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(2)").text() mustBe currentTaxYearOctoberUpdate
          }
          "has a bullet pointed list of previous updates for Q4" in {
            documentCurrentTaxYear("Q4").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(1)").text() mustBe currentTaxYearJulyUpdate
            documentCurrentTaxYear("Q4").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(2)").text() mustBe currentTaxYearOctoberUpdate
            documentCurrentTaxYear("Q4").select("#whatHappensNow > ol > li:nth-of-type(2) > ul > li:nth-child(3)").text() mustBe currentTaxYearJanuaryUpdate
          }
        }

        s"have a third numbered point $currentYaxYearQuarterlyUpdates" which {
          "has info on current updates" in {
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(3) > span").text() mustBe currentYaxYearQuarterlyUpdates
          }
          "has a bullet pointed list of current updates for Q2" in {
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(1)").text() mustBe currentTaxYearOctoberUpdate
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(2)").text() mustBe currentTaxYearJanuaryUpdate
            documentCurrentTaxYear("Q2").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(3)").text() mustBe currentTaxYearAprilUpdate
          }
          "has a bullet pointed list of current updates for Q3" in {
            documentCurrentTaxYear("Q3").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(1)").text() mustBe currentTaxYearJanuaryUpdate
            documentCurrentTaxYear("Q3").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(2)").text() mustBe currentTaxYearAprilUpdate
          }
          "has a bullet pointed list of current updates for Q4" in {
            documentCurrentTaxYear("Q4").select("#whatHappensNow > ol > li:nth-of-type(3) > ul > li:nth-child(1)").text() mustBe currentTaxYearAprilUpdate
          }
        }

        s"has a paragraph stating Income Tax Estimate '$para1'" in {
          documentNextTaxYear.select("#whatHappensNow p").get(0).text() mustBe para1
        }

        s"has a paragraph stating Info delay '$para2'" in {
          documentNextTaxYear.select("#whatHappensNow p").get(1).text() mustBe para2
        }

      }
    }

    "have a finish and sign out button" in {
      val actionSignOut = documentNextTaxYear.getElementsByClass("govuk-button")
      actionSignOut.text() mustBe finishAndSignOut
    }

  }
}
