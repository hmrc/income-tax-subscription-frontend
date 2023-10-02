/*
 * Copyright 2022 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestModels.{testFullOverseasPropertyModel, testFullPropertyModel, testTooManyBusinesses}
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.{overseasPropertyStartDateURI, propertyStartDateURI}
import helpers.agent.servicemocks.AuthStub
import models.Cash
import models.common._
import models.common.business.SelfEmploymentData
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import helpers.IntegrationTestConstants.taskListURI
import helpers.IntegrationTestModels.{testBusinesses, testFullOverseasPropertyModel, testFullPropertyModel}


class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {
  private val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"



  s"GET ${routes.YourIncomeSourceToSignUpController.show.url}" should {
    "return OK" when {
      "there are no income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSourcesAgent()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.your-income-source.title") + serviceNameGovUk)
        )
      }
      "there are multiple income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSourcesAgent()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("agent.your-income-source.title") + serviceNameGovUk)
        )
      }
    }
  }

}
