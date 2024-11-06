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
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{AgentURI, testNino}
import helpers.IntegrationTestModels.testAccountingYearCurrent
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import models.{EligibilityStatus, Yes, YesNo}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.SelectedTaxYear

class WhatYouNeedToDoControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(PrePopulate)
  }

  val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

  s"GET ${routes.WhatYouNeedToDoController.show().url}" must {
    "return OK with the page content" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.MANDATION_STATUS)(OK, Json.toJson(MandationStatusModel(Voluntary, Voluntary)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)(OK, Json.toJson(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true)))
      SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SelectedTaxYear, OK, Json.toJson(testAccountingYearCurrent))
      SessionDataConnectorStub.stubSaveSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE, Yes)(OK)


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
    "return a SEE_OTHER to the task list page" in {
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"POST ${routes.WhatYouNeedToDoController.submit.url} is called")
      val result = IncomeTaxSubscriptionFrontend.submitWhatYouNeedToDo()

      Then("The result should be SEE_OTHER redirecting to the task list page")
      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(AgentURI.taskListURI)
      )
    }

    "return a SEE_OTHER to the Your Income Sources page when PrePop enabled" in {
      enable(PrePopulate)
      Given("I am authenticated")
      AuthStub.stubAuthSuccess()

      When(s"POST ${routes.WhatYouNeedToDoController.submit.url} is called")
      val result = IncomeTaxSubscriptionFrontend.submitWhatYouNeedToDo()

      Then("The result should be SEE_OTHER redirecting to the task list page")
      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(AgentURI.yourIncomeSourcesURI)
      )
    }
  }

}
