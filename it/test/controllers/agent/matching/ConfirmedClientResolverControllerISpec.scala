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

import auth.agent.{AgentSignUp, AgentUserMatching}
import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.{EligibilityStub, ThrottlingStub}
import models.EligibilityStatus
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsBoolean, JsString}
import play.api.{Configuration, Environment}
import services.AgentStartOfJourneyThrottle
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.agent.TestConstants.{testNino, testUtr}
import utilities.{SubscriptionDataKeys, UserMatchingSessionUtil}

class ConfirmedClientResolverControllerISpec extends ComponentSpecBase with AuthRedirects with SessionCookieCrumbler {

  val session: Map[String, String] = Map(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
  }

  s"GET ${routes.ConfirmedClientResolver.resolve.url}" when {
    "the agent is unauthenticated" should {
      "redirect to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(ggLoginUrl)
        )
      }
    }

    "the agent has no nino in session" should {
      "return an internal server error" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "the throttle rejects the user" should {
      "redirect the agent to the throttle kickout page" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
        ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = true)

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.routes.ThrottlingController.start().url)
        )

        ThrottlingStub.verifyThrottle(AgentStartOfJourneyThrottle.throttleId)(1)
      }
    }

    "the throttle allows the user" when {

      "the eligibility of the client check returned an error" should {
        "return an internal server error" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          EligibilityStub.stubEligibilityResponseError(testUtr)

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }

      "the eligibility of the client check returned an ineligible for both years result" should {
        "redirect the agent to the client cannot take part page" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(NO_CONTENT)
          EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = false, nextYearResponse = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false))(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          val fullSession = session ++ Map(
            ITSASessionKeys.FailedClientMatching -> "1",
            UserMatchingSessionUtil.dobD -> "DOBD",
            UserMatchingSessionUtil.dobM -> "DOBM",
            UserMatchingSessionUtil.dobY -> "DOBY",
            UserMatchingSessionUtil.nino -> "MatchingNino",
          )

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(fullSession)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show.url)
          )

          getSessionMap(res) - "ts" - "sessionId" - "authToken" mustBe Map(ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true")
        }
      }

      "the eligibility of the client check returned eligible for next year only" should {
        "redirect to the what you are agreeing to page when the user has previously started signing up this client" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(NO_CONTENT)
          EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = false, nextYearResponse = true)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, OK, JsBoolean(true))

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )

          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
        "redirect to the sign up next year page when the user has not previously started signing up this client" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(NO_CONTENT)
          EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = false, nextYearResponse = true)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true))(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
          )

          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }

      "the eligibility of the client check returned eligible for both years" should {

        "redirect to the what you are agreeing to page when the user has previously started signing up their client" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(NO_CONTENT)
          EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = true, nextYearResponse = true)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))(OK)
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, OK, JsBoolean(true))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
          )

          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
        "redirect to the sign up next year page when the user has not previously started signing up their client" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(NO_CONTENT)
          ThrottlingStub.stubThrottle(AgentStartOfJourneyThrottle.throttleId)(throttled = false)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle), true)(OK)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(NO_CONTENT)
          EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = true, nextYearResponse = true)
          SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))(OK)

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.eligibility.routes.ClientCanSignUpController.show().url)
          )

          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }
    }

  }

  override val env: Environment = app.injector.instanceOf[Environment]
  override val config: Configuration = app.injector.instanceOf[Configuration]

}
