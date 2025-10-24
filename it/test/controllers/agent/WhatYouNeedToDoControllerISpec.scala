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

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitching
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, basGatewaySignIn, testNino}
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{EligibilityStatus, Yes, YesNo}
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.SelectedTaxYear
import utilities.agent.TestConstants.testUtr

class WhatYouNeedToDoControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.WhatYouNeedToDoController.show().url}" must {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.whatYouNeedToDo()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/what-you-need-to-do"))
        )
      }
    }
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
      SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, Yes)(OK)
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.CAPTURE_CONSENT)(NO_CONTENT)


      When(s"GET ${routes.WhatYouNeedToDoController.show().url} is called")
      val result = IncomeTaxSubscriptionFrontend.whatYouNeedToDo()

      Then("The result should be OK with page content")
      result must have(
        httpStatus(OK),
        pageTitle(messages("agent.what-you-need-to-do.heading") + serviceNameGovUk)
      )
    }
  }

  s"POST ${routes.WhatYouNeedToDoController.submit.url}" must {
    "return SEE_OTHER to the login page" when {
      "user is unauthenticated" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitWhatYouNeedToDo()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/what-you-need-to-do"))
        )
      }
    }

    "return a SEE_OTHER to the Your Income Sources page" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

      When(s"POST ${routes.WhatYouNeedToDoController.submit.url} is called")
      val result = IncomeTaxSubscriptionFrontend.submitWhatYouNeedToDo()

      Then("The result should be SEE_OTHER redirecting to Your Income Sources page")
      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(AgentURI.yourIncomeSourcesURI)
      )
    }
  }

}
