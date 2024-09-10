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

package views.agent.eligibility
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.HtmlFormat
import utilities.{AccountingPeriodUtil, ViewSpec}
import views.html.agent.eligibility.CannotSignUpThisYear

class CannotSignUpThisYearViewSpec extends ViewSpec {

  private val cannotSignUpThisYear = app.injector.instanceOf[CannotSignUpThisYear]
  val clientName: String = "FirstName LastName"
  val clientNino: String = "AA 11 11 11 A"
  val currentTaxYearStart: String = AccountingPeriodUtil.getCurrentTaxYear.startDate.toCheckYourAnswersDateFormat
  val currentTaxYearEnd: String = AccountingPeriodUtil.getCurrentTaxYear.endDate.toCheckYourAnswersDateFormat
  val nextTaxYearStart: String = AccountingPeriodUtil.getNextTaxYear.startDate.toCheckYourAnswersDateFormat
  val nextTaxYearEnd: String = AccountingPeriodUtil.getNextTaxYear.endDate.toCheckYourAnswersDateFormat

  "Cannot Sign Up View" should {

    def mainContent: Element = document(
      clientName,
      clientNino
    ).mainContent

    "have the correct template" in new TemplateViewTest(
      view = page(clientName, clientNino),
      title = CannotSignUpMessages.title,
      isAgent = true,
      backLink = None,
      hasSignOutLink = true
    )

    "have a heading caption" in {
      mainContent.selectHead("span.govuk-caption-l").text mustBe s"$clientName | $clientNino"
    }

    "have a heading" in {
      mainContent.selectFirst("h1").text mustBe CannotSignUpMessages.heading
    }

    "have a first paragraph" in {
      mainContent.selectNth("p", 1).text mustBe CannotSignUpMessages.para1(nextTaxYearStart, nextTaxYearEnd)
    }

    "have an inset paragraph" in {
      mainContent.selectHead(".govuk-inset-text").text mustBe CannotSignUpMessages.insetPara(currentTaxYearStart, currentTaxYearEnd)
    }

    "have a first subheading" in {
      mainContent.selectNth("h2", 1).text mustBe CannotSignUpMessages.subheading1
    }

    "have a second paragraph" in {
      mainContent.selectNth("p", 2).text mustBe CannotSignUpMessages.para2
    }

    "have a first bullet list" which {
      def bulletList: Element = mainContent.selectNth("ol.govuk-list--bullet", 1)

      "has a first point" in {
        bulletList.selectNth("li", 1).text mustBe CannotSignUpMessages.bullet1
      }
      "has a second point" in {
        bulletList.selectNth("li", 2).text mustBe CannotSignUpMessages.bullet2
      }
    }

    "have a third paragraph" in {
      mainContent.selectNth("p", 3).text mustBe CannotSignUpMessages.para3
    }

    "have a second bullet list" which {
      def bulletList: Element = mainContent.selectNth("ol.govuk-list--bullet", 2)

      "has a first point" in {
        bulletList.selectNth("li", 1).text mustBe CannotSignUpMessages.bullet3
      }
      "has a second point" in {
        bulletList.selectNth("li", 2).text mustBe CannotSignUpMessages.bullet4
      }
    }

    "have a second subheading" in {
      mainContent.selectNth("h2", 2).text mustBe CannotSignUpMessages.subheading2
    }

    "have a fourth paragraph" in {
      mainContent.selectNth("p", 4).text mustBe CannotSignUpMessages.para4
    }

    "have a third bullet list" which {
      def bulletList: Element = mainContent.selectNth("ol.govuk-list--bullet", 3)

      "has a first point" in {
        bulletList.selectNth("li", 1).text mustBe CannotSignUpMessages.bullet5
      }
      "has a second point" in {
        bulletList.selectNth("li", 2).text mustBe CannotSignUpMessages.bullet6
      }
    }

    "have a form" which {

      "has the correct attributes" in {
        mainContent.getForm.attr("method") mustBe testCall.method
        mainContent.getForm.attr("action") mustBe testCall.url
      }

      "has a sign up this client button" in {
        mainContent.getForm.getGovukSubmitButton.text mustBe CannotSignUpMessages.signUpThisClientButton
      }

      "has a check another client option" that {

        "has paragraph" in {
          mainContent.getForm.selectNth("p", 1).text mustBe CannotSignUpMessages.checkAnotherLink(clientName)
        }

        "contains a link" in {
          mainContent.getForm.selectNth("a.govuk-link", 1).text mustBe CannotSignUpMessages.checkAnotherClientLinkText
        }
      }
    }
  }

  def page(clientName: String,
           clientNino: String
          ): HtmlFormat.Appendable = {
    cannotSignUpThisYear(
      postAction = testCall,
      clientName = clientName,
      clientNino = clientNino
    )
  }

  def document(clientName: String, clientNino: String): Document = {
    Jsoup.parse(page(clientName, clientNino).body)
  }

  object CannotSignUpMessages {
    val title = "You can sign up this client from next tax year"
    val heading = "You can sign up this client from next tax year"
    def para1(year:String, nextyear:String):String = s"You can sign up this client for Making Tax Digital for Income Tax now. You’ll be signing them up from next tax year ($year to $nextyear)."
    def insetPara(year:String, nextyear:String):String = s"You’ll need to make sure your client submits their Self Assessment tax return for the current tax year ($year to $nextyear) as normal."
    val subheading1 = "What happens next"
    val para2 = "For each of your client’s businesses, you’ll need their:"
    val bullet1 = "business start date (or the date they started getting property income)"
    val bullet2 = "accounting method (cash basis or traditional accounting - this is also called accruals or standard accounting)"
    val para3 = "If they’re a sole trader, you’ll also need your client’s:"
    val bullet3 = "business trading name and address"
    val bullet4 = "trade (the nature of their business)"
    val subheading2 = "If you do not sign up your client now"
    val para4 = "If you do not sign up your client now, you’ll need to:"
    val bullet5 = "make sure they continue to submit their Self Assessment tax returns as normal"
    val bullet6 = "re-enter their details if you return to sign them up later"

    val signUpThisClientButton = "Sign up this client"
    val checkAnotherClientLinkText = "check if you can sign up another client"
    def checkAnotherLink(clientName:String):String = s"Or you can $checkAnotherClientLinkText. We will not save the details you entered about $clientName."
  }
}
