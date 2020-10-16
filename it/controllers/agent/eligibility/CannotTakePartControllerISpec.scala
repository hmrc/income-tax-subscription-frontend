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
    val pageContent: Element = doc.content
  }



  object CannotTakePartMessages {
    val back: String = "Back"

    val title: String = "Your client cannot take part in this pilot"
    val heading: String = "Your client cannot take part in this pilot"
    val income: String = "You will not be able to sign up your client for this pilot if they receive income from:"
    val incomePoint1: String = "being employed by another business"
    val incomePoint2: String = "UK pensions or annuities"
    val incomePoint3: String = "investments from outside the UK"
    val incomePoint4: String = "capital gains"
    val incomePoint5: String = "taxable state benefits"
    val other: String = "Your client also cannot take part if your client’s:"
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
      result.status shouldBe OK
    }

    "return a page" which {

      "has the correct title" in new Setup {
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        doc.title shouldBe CannotTakePartMessages.title + serviceNameGovUk
      }

      "has a back link" in new Setup {
        pageContent.getBackLink.text shouldBe CannotTakePartMessages.back
      }

      "has the correct heading" in new Setup {
        pageContent.getH1Element.text() shouldBe CannotTakePartMessages.heading
      }

      "has a correct paragraph for income sources" in new Setup {
        pageContent.getNthParagraph(1).text shouldBe CannotTakePartMessages.income
      }

      "has a bullet point list about incomes" which {

        "has the correct first point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(1).text shouldBe CannotTakePartMessages.incomePoint1
        }

        "has the correct second point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(2).text shouldBe CannotTakePartMessages.incomePoint2
        }

        "has the correct third point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(3).text shouldBe CannotTakePartMessages.incomePoint3
        }

        "has the correct forth point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(4).text shouldBe CannotTakePartMessages.incomePoint4
        }

        "has the correct fifth point" in new Setup {
          pageContent.getNthUnorderedList(1).getNthListItem(5).text shouldBe CannotTakePartMessages.incomePoint5
        }

      }

      "has a correct paragraph about other reasons" in new Setup {
        pageContent.getNthParagraph(2).text shouldBe CannotTakePartMessages.other
      }

      "has a bullet point list about other reasons" which {

        "has the correct first point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(1).text shouldBe CannotTakePartMessages.otherPoint1
        }

        "has the correct second point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(2).text shouldBe CannotTakePartMessages.otherPoint2
        }

        "has the correct third point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(3).text shouldBe CannotTakePartMessages.otherPoint3
        }

        "has the correct forth point" in new Setup {
          pageContent.getNthUnorderedList(2).getNthListItem(4).text shouldBe CannotTakePartMessages.otherPoint4
        }

      }

      "has a correct final paragraph" in new Setup {
        pageContent.getNthParagraph(3).text shouldBe CannotTakePartMessages.alternative
      }

      "has the correct form with button" in new Setup {
        val form: Element = pageContent.getForm
        form.attr("method") shouldBe "GET"
        form.attr("action") shouldBe controllers.agent.eligibility.routes.Covid19ClaimCheckController.show().url

        form.getSubmitButton.text shouldBe CannotTakePartMessages.signUpAnother
      }

      "has a link to sign out" in new Setup {
        val link: Element = pageContent.getLink("sign-out")

        link.attr("href") shouldBe controllers.SignOutController.signOut(controllers.agent.eligibility.routes.CannotTakePartController.show().url).url
        link.text shouldBe CannotTakePartMessages.signOut
      }

    }
  }
}
