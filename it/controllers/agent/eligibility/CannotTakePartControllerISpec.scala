/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.agent.eligibility

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class CannotTakePartControllerISpec extends ComponentSpecBase {

  trait Setup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotTakePart
    val doc: Document = Jsoup.parse(result.body)
    val pageContent: Element = doc.mainContent
  }

  object CannotTakePartMessages {
    val back: String = "Back"

    val title: String = "Your client cannot take part in this pilot"
    val heading: String = "Your client cannot take part in this pilot"
    val income: String = "You will not be able to sign up your client for this pilot if they receive income from:"
    val incomePoint1: String = "PAYE income as an employee"
    val incomePoint2: String = "UK pensions or annuities"
    val incomePoint3: String = "investments from outside the UK"
    val incomePoint4: String = "capital gains"
    val incomePoint5: String = "taxable state benefits"
    val other: String = "Your client also cannot take part if their:"
    val otherPoint1: String = "tax year does not align with the standard tax year"
    val otherPoint2: String = "sole trader business began within the last two years"
    val otherPoint3: String = "property business began within the last year"
    val otherPoint4: String = "accounting period does not align with the standard tax year"
    val alternative: String = "Your client will need to send a self assessment tax return instead. You may be able to ‘Use software to report your client’s Income Tax’ service for your client in the future."

    val signUpAnother: String = "Sign up another client"
    val signOut: String = "Sign out"
  }

  "GET /client/other-sources-of-income-error" should {

    "return a status of OK" in new Setup {
      result.status mustBe OK
    }

    "return a page" which {

      "has the correct title" in new Setup {
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        doc.title mustBe CannotTakePartMessages.title + serviceNameGovUk
      }

      "has a back button" in new Setup {
        val link: Element = doc.getGovukBackLink
        link.attr("href") mustBe "javascript:history.back()"
        link.text mustBe CannotTakePartMessages.back
      }

      "has the correct heading" in new Setup {
        pageContent.getH1Element.text() mustBe CannotTakePartMessages.heading
      }

      "has a correct paragraph for income sources" in new Setup {
        pageContent.getNthParagraph(1).text mustBe CannotTakePartMessages.income
      }

      "has a bullet point list about incomes" which {

        "has the correct first point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(1).text mustBe CannotTakePartMessages.incomePoint1
        }

        "has the correct second point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(2).text mustBe CannotTakePartMessages.incomePoint2
        }

        "has the correct third point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(3).text mustBe CannotTakePartMessages.incomePoint3
        }

        "has the correct forth point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(4).text mustBe CannotTakePartMessages.incomePoint4
        }

        "has the correct fifth point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(5).text mustBe CannotTakePartMessages.incomePoint5
        }

      }

      "has a correct paragraph about other reasons" in new Setup {
        pageContent.getNthParagraph(2).text mustBe CannotTakePartMessages.other
      }

      "has a bullet point list about other reasons" which {

        "has the correct first point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(1).text mustBe CannotTakePartMessages.otherPoint1
        }

        "has the correct second point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(2).text mustBe CannotTakePartMessages.otherPoint2
        }

        "has the correct third point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(3).text mustBe CannotTakePartMessages.otherPoint3
        }

        "has the correct forth point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(4).text mustBe CannotTakePartMessages.otherPoint4
        }

      }

      "has a correct final paragraph" in new Setup {
        pageContent.getNthParagraph(3).text mustBe CannotTakePartMessages.alternative
      }

      "has the correct form with button" in new Setup {
        val form: Element = pageContent.getForm
        form.attr("method") mustBe "GET"
        form.attr("action") mustBe controllers.agent.eligibility.routes.OtherSourcesOfIncomeController.show.url

        form.firstOf("button").text mustBe CannotTakePartMessages.signUpAnother
      }

      "has a link to sign out" in new Setup {
        val link: Element = pageContent.getLink("sign-out-button")

        link.attr("href") mustBe controllers.SignOutController.signOut.url
        link.text mustBe CannotTakePartMessages.signOut
      }

    }
  }
}
