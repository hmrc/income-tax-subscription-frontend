/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.agent

import config.featureswitch.FeatureSwitch.RemoveCovidPages
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{OK, SEE_OTHER}

class NoClientRelationshipControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(RemoveCovidPages)
    super.beforeEach()
  }

      trait Setup {
        AuthStub.stubAuthSuccess()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.getNoClientRelationship()
        val doc: Document = Jsoup.parse(result.body)
        val pageContent: Element = doc.content
      }

      object NoClientRelationshipMessages {
        val title: String = "You’re not authorised for this client"
        val heading: String = "You’re not authorised for this client"
        val para1: String = "To authorise you as their agent, your client needs to sign into this service (opens in new tab) using their own Government Gateway details. Once they have done this, you can come back to sign up your client."
        val link: String = "sign into this service (opens in new tab)"
        val button: String = "Sign up another client"
        val signOut: String = "Sign out"
      }

      "GET /error/no-client-relationship" should {
        "return OK" in new Setup {
          result should have(
            httpStatus(OK)
          )
        }

        "have a view with the correct title" in new Setup {
          val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
          doc.title shouldBe NoClientRelationshipMessages.title + serviceNameGovUk
        }

        "have a view with the correct heading" in new Setup {
          pageContent.getH1Element.text shouldBe NoClientRelationshipMessages.heading
        }

        "have a paragraph explaining why they are not authorised" in new Setup {
          pageContent.getNthParagraph(1).text shouldBe NoClientRelationshipMessages.para1
        }

        "have a Sign up another client button" in new Setup {
          val submitButton: Element = pageContent.getForm.getSubmitButton
          submitButton.text shouldBe NoClientRelationshipMessages.button
          submitButton.attr("class") shouldBe "button"
          submitButton.attr("type") shouldBe "submit"
        }

        "have a Sign Out link" in new Setup {
          val signOutLink: Element = pageContent.getLink("sign-out")
          signOutLink.attr("href") shouldBe controllers.SignOutController.signOut.url
          signOutLink.text shouldBe NoClientRelationshipMessages.signOut
        }

        "have a view with a link" in new Setup {
          val link: Element = doc.getLink("get-agent-authorisation")
          link.attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview"
          link.text shouldBe NoClientRelationshipMessages.link
        }

        "return SEE_OTHER when selecting clicking sign up another client" in new Setup{

          val res = IncomeTaxSubscriptionFrontend.postNoClientRelationship()
          val expectedRedirect: String = "/report-quarterly/income-and-expenses/sign-up/client/eligibility/covid-19"

          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirect)
          )

        }

        "return SEE_OTHER when selecting clicking sign up another client - RemoveCovidPages FS enabled" in new Setup{

          enable(RemoveCovidPages)
          val res = IncomeTaxSubscriptionFrontend.postNoClientRelationship()
          val expectedRedirect: String = "/report-quarterly/income-and-expenses/sign-up/client/eligibility/income-sources"

          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirect)
          )

        }
    }
}
