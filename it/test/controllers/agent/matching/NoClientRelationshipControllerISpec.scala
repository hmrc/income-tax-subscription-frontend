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
import controllers.agent.routes
import helpers.IntegrationTestConstants.testNino
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse

class NoClientRelationshipControllerISpec extends ComponentSpecBase  {

  class Setup(clientDetailsConfirmed: Boolean = true) {
    AuthStub.stubAuthSuccess()

    SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

    val result: WSResponse = IncomeTaxSubscriptionFrontend.getNoClientRelationship(clientDetailsConfirmed)

    val doc: Document = Jsoup.parse(result.body)
  }

  object NoClientRelationshipMessages {
    val title: String = "There is a problem"
    val heading: String = "There is a problem"
    val para1: String ="We cannot find your client’s authorisation in your agent services account."
    val para2: String = "You need to:"
    val bullet1: String = "Check your " + "agent services account (opens in new tab)"
    val bullet2: String = "Make sure you have copied across all existing authorisations for all your clients and all your Government Gateway user IDs."
    val bullet3: String = "If you still do not have this client’s authorisation, you’ll need to get a new authorisation from them."
    val para3: String = "When you have your client’s authorisation, you can come back to sign them up."
    val button: String = "Sign up another client"
  }

  "GET /error/no-client/relationship" should {
    "return SEE_OTHER (303)" when {
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
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
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
      private val bulletPoints = doc.body().select(".govuk-list.govuk-list--number li")
      bulletPoints.size mustBe 3
      bulletPoints.get(0).text mustBe NoClientRelationshipMessages.bullet1
      bulletPoints.get(1).text mustBe NoClientRelationshipMessages.bullet2
      bulletPoints.get(2).text mustBe NoClientRelationshipMessages.bullet3
    }

    "have the third paragraph" in new Setup() {
      private val content = doc.body().getElementById("main-content")
      content.getNthParagraph(3).text mustBe NoClientRelationshipMessages.para3
    }

    "have a Sign up another client button" in new Setup() {
      private val content = doc.body().getElementById("main-content")
      val submitButton: Element = content.getForm.getGovUkSubmitButton
      submitButton.text mustBe NoClientRelationshipMessages.button
    }

    "have a view with a link" in new Setup() {
      doc.mainContent.selectHead("a").attr("href") mustBe "https://www.tax.service.gov.uk/agent-services-account/home#tax-services-accordion-content-1"
    }

    "return SEE_OTHER when selecting clicking sign up another client" in new Setup() {

      private val res = IncomeTaxSubscriptionFrontend.postNoClientRelationship()
      val expectedRedirect: String = routes.AddAnotherClientController.addAnother().url

      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(expectedRedirect)
      )

    }
  }
}
