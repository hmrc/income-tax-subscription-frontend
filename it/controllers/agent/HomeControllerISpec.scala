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
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import helpers.agent.IntegrationTestConstants._
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
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("the result must have a status of SEE_OTHER and a redirect location of /client-details")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(clientDetailsURI)
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
            redirectURI(clientDetailsURI)
          )

          Then("the JourneyStateKey should remain as UserMatching")
          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentUserMatching.name)
        }
      }

      "journey state is UserMatched" when {

        "the matched user has a utr" should {
          "redirect to client details" when {
            "save and retrieve is enabled" should {
              "redirect to task list page" in {
                Given("I setup the wiremock stubs")
                AuthStub.stubAuthSuccess()
                enable(SaveAndRetrieve)

                When("I call GET /index")
                val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino, ITSASessionKeys.UTR -> testUtr))

                Then("the result must have a status of SEE_OTHER and a redirect location of /task-list")
                res must have(
                  httpStatus(SEE_OTHER),
                  redirectURI(taskListURI)
                )

                Then("the JourneyStateKey should be changed to AgentSignUp")
                getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
              }
            }

            "save and retrieve is disabled" should {
              "redirect to what tax year to sign up page" in {
                Given("I setup the wiremock stubs")
                AuthStub.stubAuthSuccess()
                disable(SaveAndRetrieve)

                When("I call GET /index")
                val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatched), Map(ITSASessionKeys.NINO -> testNino, ITSASessionKeys.UTR -> testUtr))

                Then("the result must have a status of SEE_OTHER and a redirect location of /what-year-to-sign-up")
                res must have(
                  httpStatus(SEE_OTHER),
                  redirectURI(whatYearToSignUpURI)
                )

                Then("the JourneyStateKey should be changed to AgentSignUp")
                getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
              }
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
              redirectURI(registerForSAURI)
            )

            Then("the JourneyStateKey should be removed")
            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe None
          }
        }

      }

    }
  }
}
