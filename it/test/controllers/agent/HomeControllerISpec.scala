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

import auth.agent.{AgentSignUp, AgentUserMatched, AgentUserMatching}
import common.Constants.ITSASessionKeys
import helpers.IntegrationTestConstants.{AgentURI, testNino, testUtr}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import play.api.http.Status._

class HomeControllerISpec extends ComponentSpecBase with SessionCookieCrumbler  {

  "GET /" should {
    "return a redirect to the index page" in {
      Given("I setup the wiremock stubs")

      When("I call GET /")
      val res = IncomeTaxSubscriptionFrontend.startPage()

      Then("the result must have a status of SEE_OTHER and the front page title")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.routes.HomeController.index.url)
      )
    }
  }

  "GET /index" when {
    "auth is successful" when {
      "journey state is not in session" should {
        "redirect to client details" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("I call GET /index")
          val res = IncomeTaxSubscriptionFrontend.indexPage(None)

          Then("the result must have a status of SEE_OTHER and a redirect location of /client-details")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.clientDetailsURI)
          )

          Then("the JourneyStateKey should be added as UserMatching")
          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
        }
      }

      "journey state is UserMatching" should {
        "redirect to client details when the agent is authorised" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When("I call GET /index")
          val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatching))

          Then("the result must have a status of SEE_OTHER and a redirect location of /client-details")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(AgentURI.clientDetailsURI)
          )

          Then("the JourneyStateKey should remain as UserMatching")
          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
        }
      }

      "journey state is UserMatched" when {
        "the matched user has a utr" should {
          "redirect to client details" should {
            "redirect to what you need to do page" in {
              Given("I setup the wiremock stubs")
              AuthStub.stubAuthSuccess()

              When("I call GET /index")
              val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino, ITSASessionKeys.UTR -> testUtr))

              Then("the result must have a status of SEE_OTHER and a redirect location of /what-you-need-to-do")
              res must have(
                httpStatus(SEE_OTHER),
                redirectURI(AgentURI.whatYouNeedToDoURI)
              )

              Then("the JourneyStateKey should be changed to AgentSignUp")
              getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
            }
          }
        }

        "the matched user only has a nino" should {
          "redirect to client details" in {
            Given("I setup the wiremock stubs")
            AuthStub.stubAuthSuccess()

            When("I call GET /index")
            val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino))

            Then("the result must have a status of SEE_OTHER and a redirect location of /register-for-SA")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(AgentURI.registerForSAURI)
            )

            Then("the JourneyStateKey should be removed")
            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe None
          }
        }
      }
    }
  }
}
