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
import config.featureswitch.FeatureSwitch.{PrePopulate, ThrottlingFeature}
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.{EligibilityStub, PrePopStub, ThrottlingStub}
import models.agent.JourneyStep.SignPosted
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import models.{Accruals, Cash, DateModel, EligibilityStatus}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsBoolean, JsString, Json}
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
    disable(PrePopulate)
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
        s"redirect the agent to the client cannot take part page and set the journey state to ${SignPosted.key}" in {
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

          getSessionMap(res) - "ts" - "sessionId" - "authToken" mustBe Map(
            ITSASessionKeys.CLIENT_DETAILS_CONFIRMED -> "true",
            ITSASessionKeys.JourneyStateKey -> SignPosted.key
          )
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

  s"GET ${routes.ConfirmedClientResolver.resolve.url}" when {
    "the pre-pop feature switch is enabled" should {
      "pre-pop income sources and continue as normal" in {
        enable(PrePopulate)

        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(OK, JsBoolean(true))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(
          responseStatus = OK,
          responseBody = Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, NO_CONTENT)
        PrePopStub.stubGetPrePop(testNino)(
          status = OK,
          body = Json.obj(
            "selfEmployment" -> Json.arr(
              Json.obj(
                "name" -> "ABC",
                "trade" -> "Plumbing",
                "address" -> Json.obj(
                  "lines" -> Json.arr(
                    "1 long road"
                  ),
                  "postcode" -> "ZZ1 1ZZ"
                ),
                "startDate" -> Json.obj(
                  "day" -> "01",
                  "month" -> "02",
                  "year" -> "2000"
                ),
                "accountingMethod" -> "cash"
              )
            ),
            "ukPropertyAccountingMethod" -> "accruals",
            "foreignPropertyAccountingMethod" -> "cash"
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.PrePopFlag, true)
        IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinessDetails(
          selfEmployments = Seq(SelfEmploymentData(
            id = "test-uuid",
            businessStartDate = Some(BusinessStartDate(DateModel("01", "02", "2000"))),
            businessName = Some(BusinessNameModel("ABC")),
            businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
            businessAddress = Some(BusinessAddressModel(Address(
              lines = Seq(
                "1 long road"
              ),
              postcode = Some("ZZ1 1ZZ")
            )))
          )),
          accountingMethod = Some(Cash)
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
          PropertyModel(accountingMethod = Some(Accruals))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
          OverseasPropertyModel(accountingMethod = Some(Cash))
        )

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.ClientCanSignUpController.show().url)
        )

        getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
      }
      "result in technical difficulties" when {
        "there was an error returned from the pre-pop connection" in {
          enable(PrePopulate)

          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle))(OK, JsBoolean(true))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(
            responseStatus = OK,
            responseBody = Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, NO_CONTENT)
          PrePopStub.stubGetPrePop(testNino)(
            status = INTERNAL_SERVER_ERROR,
            body = Json.obj()
          )

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

  override val env: Environment = app.injector.instanceOf[Environment]
  override val config: Configuration = app.injector.instanceOf[Configuration]

}
