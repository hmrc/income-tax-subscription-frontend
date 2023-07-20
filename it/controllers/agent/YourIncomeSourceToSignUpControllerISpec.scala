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


class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {
  private val serviceNameGovUk = "Use software to report your client’s Income Tax - GOV.UK"
  "GET /report-quarterly/income-and-expenses/sign-up/client/your-income-source" should {
    "return OK" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

      When(s"GET ${routes.YourIncomeSourceToSignUpController.show().url} is called")
      val res = IncomeTaxSubscriptionFrontend.agentBusinessIncomeSource()

      Then("Should return OK with the income source page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.your-income-source.heading")} - $serviceNameGovUk"
        )
      )
    }
  }
}
