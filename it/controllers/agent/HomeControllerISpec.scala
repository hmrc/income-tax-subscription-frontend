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

import _root_.agent.auth.{AgentSignUp, AgentUserMatched, AgentUserMatching}
import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.servicemocks.AuthStub
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status._
import play.api.i18n.Messages

class HomeControllerISpec extends ComponentSpecBase {

  "GET /" when {
    "feature-switch.show-guidance is true" should {
      "return the guidance page" in {
        Given("I setup the wiremock stubs")

        When("I call GET /")
        val res = IncomeTaxSubscriptionFrontend.startPage()

        Then("the result should have a status of OK and the front page title")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.frontpage.title"))
        )
      }
    }
  }

  "GET /index" when {
    "auth is successful" when {
      "journey state is not in session" should {
        "redirect to client details" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("I call GET /index")
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("the result should have a status of SEE_OTHER and a redirect location of /client-details")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(clientDetailsURI)
          )

          Then("the JourneyStateKey should be added as UserMatching")
          SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe Some(AgentUserMatching.name)
        }
      }

      "journey state is UserMatching" should {
        "redirect to client details when the agent is authorised" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("I call GET /index")
          val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatching))

          Then("the result should have a status of SEE_OTHER and a redirect location of /client-details")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(clientDetailsURI)
          )

          Then("the JourneyStateKey should remain as UserMatching")
          SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe Some(AgentUserMatching.name)
        }

        "redirect to the 'not authorised' page when the agent is unauthorised" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("I call GET /index")
          val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatching),
            Map(ITSASessionKeys.UnauthorisedAgentKey -> true.toString,
              ITSASessionKeys.NINO -> testNino,
              ITSASessionKeys.UTR -> testUtr))

          Then("the result should have a status of SEE_OTHER and a redirect location of /not-authorised")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(errorNotAuthorisedURI)
          )

          Then("the JourneyStateKey should remain as UserMatching")
          SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe Some(AgentUserMatching.name)
        }
      }

      "journey state is UserMatched" when {

        "the matched user has a utr" should {
          "redirect to client details" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            When("I call GET /index")
            val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino, ITSASessionKeys.UTR -> testUtr))

            Then("the result should have a status of SEE_OTHER and a redirect location of /income")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(incomeSourceURI)
            )

            Then("the JourneyStateKey should be changed to AgentSignUp")
            SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe Some(AgentSignUp.name)
          }
        }

        "the matched user only has a nino" should {
          "redirect to client details" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            When("I call GET /index")
            val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino))

            Then("the result should have a status of SEE_OTHER and a redirect location of /register-for-SA")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(registerForSAURI)
            )

            Then("the JourneyStateKey should be removed")
            SessionCookieCrumbler.getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) shouldBe None
          }
        }

      }

    }
  }
}
