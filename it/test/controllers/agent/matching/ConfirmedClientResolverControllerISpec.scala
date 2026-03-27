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
import config.MockConfig.appConfig.ggLoginUrl
import config.featureswitch.FeatureSwitch.{ThrottlingFeature, WhenDoYouWantToStartPage}
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import helpers.servicemocks.PrePopStub
import models.EligibilityStatus
import models.common.business.*
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.{MandationStatus, MandationStatusModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsBoolean, JsString, Json}
import services.AgentStartOfJourneyThrottle
import utilities.SubscriptionDataKeys
import utilities.agent.TestConstants.{testNino, testUtr}

class ConfirmedClientResolverControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  val session: Map[String, String] = Map(
    ITSASessionKeys.JourneyStateKey -> AgentUserMatching.name
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(ThrottlingFeature)
    disable(WhenDoYouWantToStartPage)
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
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle) -> JsBoolean(true)
        ))

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"GET ${routes.ConfirmedClientResolver.resolve.url}" when {
    "the client is ineligible" should {
      "redirect to the cannot take part page" in {
        AuthStub.stubAuthSuccess()
        stubFullSession(eligibleCurrent = false, eligibleNext = false)

        val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.agent.eligibility.routes.CannotTakePartController.show.url)
        )
      }
    }
    "the WhenDoYouWantToStartPage feature switch is enabled" when {
      "the user is eligible and voluntary for both current year and next year" should {
        "redirect to the WhenDoYouWantToStartController page" in {
          enable(WhenDoYouWantToStartPage)

          AuthStub.stubAuthSuccess()
          stubFullSession()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
          stubFullPrePop()

          val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.tasklist.taxyear.routes.WhenDoYouWantToStartController.show().url)
          )

          getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
        }
      }
    }
    "the WhenDoYouWantToStartPage feature switch is disabled" when {
      "there is an eligibility interrupt flag set" when {
        "the user is mandated for the current tax year" should {
          "go to the what you need to do page" in {
            AuthStub.stubAuthSuccess()
            stubFullSession(statusCurrent = Mandated)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, OK, JsBoolean(true))
            stubFullPrePop()

            val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            )

            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
          }
        }
        "the user is eligible for the next year only" should {
          "go to the what you need to do page" in {
            AuthStub.stubAuthSuccess()
            stubFullSession(eligibleCurrent = false)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, OK, JsBoolean(true))
            stubFullPrePop()

            val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.routes.WhatYouNeedToDoController.show().url)
            )

            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
          }
        }
        "the user is neither mandated for the current tax year or eligible for the next tax year only" should {
          "go to the what year to sign up page" in {
            AuthStub.stubAuthSuccess()
            stubFullSession()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, OK, JsBoolean(true))
            stubFullPrePop()

            val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show().url)
            )

            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
          }
        }
      }
      "there is no eligibility interrupt flag set" when {
        "the user is eligible for the next year only" should {
          "go to the cannot sign up this year page" in {
            AuthStub.stubAuthSuccess()
            stubFullSession(eligibleCurrent = false)
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
            stubFullPrePop()

            val res = IncomeTaxSubscriptionFrontend.getConfirmedClientResolver(session)

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
            )

            getSessionMap(res).get(ITSASessionKeys.JourneyStateKey) mustBe Some(AgentSignUp.name)
          }
        }
        "the user is eligible for both years" should {
          "go to the client can sign up page" in {
            AuthStub.stubAuthSuccess()
            stubFullSession()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
            stubFullPrePop()

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
    "result in technical difficulties" when {
      "there was an error returned from the pre-pop connection" in {
        AuthStub.stubAuthSuccess()
        stubFullSession()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.EligibilityInterruptPassed, NO_CONTENT)
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

  def stubFullSession(eligibleCurrent: Boolean = true,
                      eligibleNext: Boolean = true,
                      statusCurrent: MandationStatus = Voluntary,
                      statusNext: MandationStatus = Voluntary): Unit = {
    SessionDataConnectorStub.stubGetAllSessionData(Map(
      ITSASessionKeys.throttlePassed(AgentStartOfJourneyThrottle) -> JsBoolean(true),
      ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(statusCurrent, statusNext)),
      ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(
        eligibleCurrentYear = eligibleCurrent, eligibleNextYear = eligibleNext, exemptionReason = None
      )),
      ITSASessionKeys.NINO -> JsString(testNino),
      ITSASessionKeys.UTR -> JsString(testUtr)
    ))
  }

  def stubFullPrePop(): Unit = {
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
            )
          )
        )
      )
    )
    IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)
    IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.PrePopFlag, true)
    IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinessDetails(
      selfEmployments = Seq(SelfEmploymentData(
        id = "test-uuid",
        startDateBeforeLimit = Some(true),
        businessName = Some(BusinessNameModel("ABC")),
        businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
        businessAddress = Some(BusinessAddressModel(Address(
          lines = Seq(
            "1 long road"
          ),
          postcode = Some("ZZ1 1ZZ"),
          country = None
        )))
      ))
    )
  }

}
