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

package controllers.individual.matching

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.ThrottlingFeature
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import models.common.business.{Address, SelfEmploymentData}
import models.common.{OverseasPropertyModel, PropertyModel}
import models.individual.JourneyStep.PreSignUp
import models.prepop.{PrePopData, PrePopSelfEmployment}
import models.{DateModel, EligibilityStatus}
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsString, Json}
import services.StartOfJourneyThrottleId
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.PrePopFlag

class HomeControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ThrottlingFeature)
  }

  s"GET ${routes.HomeController.index.url}" must {
    "redirect to the login page" when {
      "the user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/"))
        )
      }
    }
    "redirect to home and add the pre sign up journey step into session" when {
      "the user has no journey state" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.indexPage(includeState = false)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.baseURI)
        )

        val session: Map[String, String] = getSessionMap(res)
        session.get(ITSASessionKeys.JourneyStateKey) mustBe Some(PreSignUp.key)
      }
    }
    "redirect to the no self assessment page" when {
      "a utr was not found on the users cred, in session or from citizen details" in {
        AuthStub.stubAuthNoUtr()
        SessionDataConnectorStub.stubGetAllSessionData(Map())
        CitizenDetailsStub.stubCIDUserWithNoUtr(testNino)

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.noSaURI)
        )
      }
    }
    "redirect to the start of journey throttle page" when {
      "the throttle is enabled and the user hit the limit" in {
        enable(ThrottlingFeature)

        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)
        SessionDataConnectorStub.stubGetAllSessionData(Map())
        ThrottlingStub.stubThrottle(StartOfJourneyThrottleId)(throttled = true)

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.startOfJourneyThrottleURI)
        )

        ThrottlingStub.verifyThrottle(StartOfJourneyThrottleId)()
      }
    }
    "redirect to the start of the claim enrolment journey" when {
      "the user is already signed up for MTD ITSA and has no enrolment" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)
        SubscriptionStub.stubGetSubscriptionFound()

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.addMTDITOverviewURI)
        )
      }
    }
    "redirect to the ineligible to sign up page" when {
      "the user is ineligible to sign up for the current and next tax years" in {
        AuthStub.stubAuthSuccess()

        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetNoSubscription()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(ineligibleStatus)
        ))
        EligibilityStub.stubEligibilityResponseBoth(testUtr)(currentYearResponse = false, nextYearResponse = false)
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, ineligibleStatus)(OK)

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.notEligibleURI)
        )
      }
    }
    "redirect to the you can sign up now page" when {
      "the user is eligible to sign up for both tax years" in {
        AuthStub.stubAuthSuccess()

        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetNoSubscription()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(eligibleBothYears)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(PrePopFlag, NO_CONTENT)
        PrePopStub.stubGetPrePop(testNino)(OK, Json.toJson(fullPrePopData))
        IncomeTaxSubscriptionConnectorStub.stubSavePrePopFlag()

        IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinessDetails(selfEmploymentDetails)
        IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel())
        IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(OverseasPropertyModel())
        IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.routes.YouCanSignUpController.show.url)
        )
      }
    }
    "redirect to the cannot sign up for current tax year page" when {
      "the user is eligible to sign up for next tax year only" in {
        AuthStub.stubAuthSuccess()

        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetNoSubscription()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(eligibleNextYearOnly)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(PrePopFlag, OK, JsBoolean(true))

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.cannotSignUpForCurrentYearURI)
        )
      }
    }
    "return an internal server error" when {
      "there was a problem when checking if the user is already signed up" in {
        AuthStub.stubAuthSuccess()
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetSubscriptionFail()

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "there was a problem when pre-populating the users income sources" in {
        AuthStub.stubAuthSuccess()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.UTR -> JsString(testUtr)
        ))
        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetNoSubscription()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(eligibleBothYears)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(PrePopFlag, NO_CONTENT)
        PrePopStub.stubGetPrePop(testNino)(INTERNAL_SERVER_ERROR, Json.obj())

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "add the users utr and name to session" when {
      "fetched from citizen details during the utr lookup" in {
        AuthStub.stubAuthNoUtr()

        SessionDataConnectorStub.stubGetAllSessionData(Map())
        CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)

        SessionDataConnectorStub.stubSaveSessionData(ITSASessionKeys.UTR, testUtr)(OK)

        SubscriptionStub.stubGetNoSubscription()

        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(eligibleBothYears)
        ))

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(PrePopFlag, OK, JsBoolean(true))

        val res = IncomeTaxSubscriptionFrontend.indexPage()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.routes.YouCanSignUpController.show.url)
        )

        val session: Map[String, String] = getSessionMap(res)

        session.get(ITSASessionKeys.FULLNAME) mustBe Some(s"$testFirstName $testLastName")
      }
    }
  }

  lazy val ineligibleStatus: EligibilityStatus = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = false)
  lazy val eligibleNextYearOnly: EligibilityStatus = EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true)
  lazy val eligibleBothYears: EligibilityStatus = EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)

  lazy val fullPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(prePopSelfEmployment))
  )

  lazy val prePopSelfEmployment: PrePopSelfEmployment = PrePopSelfEmployment(
    name = Some("test-name"),
    trade = Some("test-trade"),
    address = Some(Address(
      lines = Seq("1 long road"),
      postcode = Some("ZZ1 1ZZ")
    )),
    startDate = Some(DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit))
  )

  lazy val selfEmploymentDetails: Seq[SelfEmploymentData] = Seq(
    prePopSelfEmployment.toSelfEmploymentData("test-uuid")
  )

}