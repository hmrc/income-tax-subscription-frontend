/*
 * Copyright 2020 HM Revenue & Customs
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

class SignUpCompleteViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val duration: Int = 0
  val action: Call = ViewSpecTrait.testCall
  val incomeSourceBusinessNextTaxYear: IncomeSourceModel = IncomeSourceModel(true, false, false)
  val incomeSourceBoth: IncomeSourceModel = IncomeSourceModel(true, true, false)
  val request: FakeRequest[AnyContentAsEmpty.type] = ViewSpecTrait.viewTestRequest
  val declarationDate = "2022"

  def page(incomeSource: IncomeSourceModel): Html = views.html.individual.incometax.subscription.sign_up_complete(
    journeyDuration = duration,
    summary = incomeSource match {
      case IncomeSourceModel(true, false, false) => testSummaryDataBusinessNextTaxYear
      case _ => testSummaryData
    },
    declarationYear = declarationDate
  )(request, implicitly, appConfig)

  def documentCurrentTaxYear: Document = Jsoup.parse(page(incomeSourceBoth).body)

  def documentNextTaxYear: Document = Jsoup.parse(page(incomeSourceBusinessNextTaxYear).body)

  val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"

  "The Sign up confirmation page" should {

    s"have the title '${MessageLookup.SignUpComplete.title}'" in {
      documentCurrentTaxYear.title() must be(MessageLookup.SignUpComplete.title + serviceNameGovUk
      )
    }

    "have a successful transaction confirmation banner" which {

      "has a green background" in {
        documentCurrentTaxYear.select("#confirmation-heading").hasClass("govuk-panel--confirmation") mustBe true
      }

      s"has a heading (H1)" which {

        lazy val heading = documentCurrentTaxYear.select("H1")

        s"has the text '${MessageLookup.SignUpComplete.heading}'" in {
          heading.text() mustBe MessageLookup.SignUpComplete.heading
        }

        "has the class 'transaction-banner__heading'" in {
          heading.hasClass("transaction-banner__heading") mustBe true
        }
      }
    }

    "have a 'What happens now' section" which {

      s"has the section heading '${MessageLookup.SignUpComplete.whatHappensNow.heading}'" in {
        documentCurrentTaxYear.select("#whatHappensNow h2").text() mustBe MessageLookup.SignUpComplete.whatHappensNow.heading
      }

      s"has a numeric list of actions for the Individual to perform if selected current tax year" in {
        val list = documentCurrentTaxYear.select("#actionList li")
        list.get(0).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number1
        list.get(0).select("a").attr("href") mustBe appConfig.softwareUrl
        list.get(1).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number2
        list.get(2).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number3
        list.get(3).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number4
        list.get(4).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number5
        list.get(5).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number6
      }

      s"has a numeric list of actions for the Individual to perform if selected next tax year" in {
        val list = documentNextTaxYear.select("#actionList li")
        list.get(0).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number1
        list.get(0).select("a").attr("href") mustBe appConfig.softwareUrl
        list.get(1).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number2
        list.get(2).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number7
        list.get(2).select("a").attr("href") mustBe appConfig.btaUrl
        list.get(3).text mustBe MessageLookup.SignUpComplete.whatHappensNow.number6
      }

      s"has a paragraph referring to Income Tax Estimate and link to BTA" in {
        val para1 = documentCurrentTaxYear.select("#whatHappensNow p").get(0)
        para1.text() must include(MessageLookup.SignUpComplete.whatHappensNow.para1)
        para1.select("a").attr("href") mustBe appConfig.btaUrl
      }

      s"does have a paragraph stating information to appear '${MessageLookup.SignUpComplete.whatHappensNow.para2}'" in {
        documentCurrentTaxYear.select("#whatHappensNow p").get(1).text() must include(MessageLookup.SignUpComplete.whatHappensNow.para2)
      }
    }

    "have a sign out button" in {
      val actionSignOut = documentCurrentTaxYear.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.SignUpComplete.whatHappensNow.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut(request.path).url
    }

  }
}
