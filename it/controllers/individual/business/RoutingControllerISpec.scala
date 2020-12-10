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

package controllers.individual.business

import config.featureswitch.FeatureSwitch.ForeignProperty
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.subscriptionData
import helpers.servicemocks.AuthStub
import models.common.IncomeSourceModel
import models.{Accruals, Cash}
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class RoutingControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /report-quarterly/income-and-expenses/sign-up/business/routing" when {

    "the Subscription Details Connector returns all data" should {
      "show the property commencement date page with UK property option selected" in {
        Given("I setup the Wiremock stubs")

        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(incomeSource = Some(IncomeSourceModel(false, true, false))))

        When("GET /business/routing is called")
        val res = IncomeTaxSubscriptionFrontend.getRouting()


        Then("Should return a SEE_OTHER with the property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(propertyCommencementDateURI)
        )
      }
      "show the overseas property commencement page with overseas property option selected" in {
        Given("I setup the Wiremock stubs")

        enable(ForeignProperty)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(incomeSource = Some(IncomeSourceModel(false, false, true))))

        When("GET /business/routing is called")
        val res = IncomeTaxSubscriptionFrontend.getRouting()


        Then("Should return a SEE_OTHER with the overseas property commencement date page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(overseasPropertyCommencementDateURI)
        )
      }
      "show the check your answers page with only self-employed option selected" in {
        Given("I setup the Wiremock stubs")

        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(incomeSource = Some(IncomeSourceModel(true, false, false))))

        When("GET /business/routing is called")
        val res = IncomeTaxSubscriptionFrontend.getRouting()


        Then("Should return a SEE_OTHER with the check your answers page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}
