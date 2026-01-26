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

package controllers.agent.matching

import common.Constants.ITSASessionKeys
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse

class NoClientRelationshipControllerISpec extends ComponentSpecBase {

  class Setup(clientDetailsConfirmed: Boolean = true) {
    AuthStub.stubAuthSuccess()

    SessionDataConnectorStub.stubGetAllSessionData(Map(
      ITSASessionKeys.NINO -> JsString(testNino)
    ))

    val result: WSResponse = IncomeTaxSubscriptionFrontend.getNoClientRelationship(clientDetailsConfirmed)

    val doc: Document = Jsoup.parse(result.body)
  }

  object NoClientRelationshipMessages {
    val title: String = "You are not authorised by your client"
    val heading: String = "You are not authorised by your client"
    val para1: String = "You need to add your client’s authorisation to your agent services account before you can continue signing them up to Making Tax Digital for Income Tax."
    val para2: String = "You can either:"
    val bullet1: String = "add your client’s authorisation (opens in new tab)"
    val bullet2: String = "Sign up another client"
    val para3: String = "You cannot sign up clients if they have not already registered for Self Assessment."
  }

  "GET /error/no-client/relationship" should {
    "return SEE_OTHER (303)" when {
      "the user is not authenticated" in {
        AuthStub.stubUnauthorised()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.getNoClientRelationship(true)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/no-client-relationship"))
        )
      }
      "the client details haven't been confirmed" in new Setup(false) {
        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }
  }

  "GET /error/no-client-relationship" should {
    "return OK" in new Setup() {
      result must have(
        httpStatus(OK)
      )
    }

    "have a view with the correct title" in new Setup() {
      val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"
      doc.title mustBe NoClientRelationshipMessages.title + serviceNameGovUk
    }

    "have a view with the correct heading" in new Setup() {
      doc.body().getH1Element.text mustBe NoClientRelationshipMessages.heading
    }

    "have the first paragraph" in new Setup() {
      private val content = doc.body().getElementById("main-content")
      content.getNthParagraph(1).text mustBe NoClientRelationshipMessages.para1
    }

    "have the second paragraph" in new Setup() {
      private val content = doc.body().getElementById("main-content")
      content.getNthParagraph(2).text mustBe NoClientRelationshipMessages.para2
    }

    "have a list of bullet points" in new Setup() {
      private val bulletPoints = doc.body().select(".govuk-list.govuk-list--bullet li")
      bulletPoints.size mustBe 2
      bulletPoints.get(0).text mustBe NoClientRelationshipMessages.bullet1
      bulletPoints.get(1).text mustBe NoClientRelationshipMessages.bullet2
    }

    "have the third inset paragraph" in new Setup() {
      private val content = doc.body().getElementById("main-content")
      content.select("div").get(3).text mustBe NoClientRelationshipMessages.para3
    }

    "have a view with a link" in new Setup() {
      doc.mainContent.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/agent-client-relationships/authorisation-request"
    }

    "have a view with a link 2" in new Setup() {
      doc.mainContent.selectNth("a", 2).attr("href") mustBe controllers.agent.routes.AddAnotherClientController.addAnother().url
    }

    "return SEE_OTHER when selecting clicking sign up another client" in new Setup() {

      private val res = IncomeTaxSubscriptionFrontend.postNoClientRelationship()
      val expectedRedirect: String = controllers.agent.routes.AddAnotherClientController.addAnother().url

      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(expectedRedirect)
      )

    }
  }
}
