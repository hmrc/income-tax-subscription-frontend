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

import auth.agent.AgentUserMatching
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.JourneyStateKey
import connectors.stubs.SessionDataConnectorStub
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsString
import play.api.libs.ws.WSResponse
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

class CannotTakePartControllerISpec extends ComponentSpecBase with AuthRedirects with SessionCookieCrumbler {

  class Setup(sessionData: Map[String, String] = ClientData.clientDataWithNinoAndUTR ++ Map(JourneyStateKey -> AgentUserMatching.name)) {
    AuthStub.stubAuthSuccess()
    SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotTakePart(sessionData)
    lazy val doc: Document = Jsoup.parse(result.body)
    lazy val pageContent: Element = doc.mainContent
  }

  object CannotTakePartMessages {
    val title: String = "You cannot sign up this client voluntarily"
  }

  "GET /client/cannot-sign-up" should {

    "return a redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotTakePart()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/error/cannot-sign-up"))
        )
      }
    }

    "return a status of OK" in new Setup() {
      result.status mustBe OK
    }

    "return a page" which {

      "has the correct title" in new Setup() {
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        doc.title mustBe CannotTakePartMessages.title + serviceNameGovUk
      }

    }

    "when the back button is pressed from 'enter client details' page" should {
      "redirect the client to 'you cannot go back to previous client' page" in new Setup(ClientData.basicClientData) {

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.matching.routes.CannotGoBackToPreviousClientController.show.url)
        )
      }
    }

  }

  override val env: Environment = app.injector.instanceOf[Environment]
  override val config: Configuration = app.injector.instanceOf[Configuration]
}
