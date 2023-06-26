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

package controllers.individual.incomesource

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import controllers.individual.business.{routes => businessRoutes}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{overseasPropertyStartDateURI, propertyStartDateURI}
import helpers.IntegrationTestModels.{testFullOverseasPropertyModel, testFullPropertyModel, testTooManyBusinesses}
import helpers.servicemocks.AuthStub
import models.Cash
import models.common._
import models.common.business.SelfEmploymentData
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys

class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase  {
  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignPropertyFeature)
  }

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  "GET /report-quarterly/income-and-expenses/sign-up/details/your-income-source" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

      When("GET /details/your-income-source is called")
      val res = IncomeTaxSubscriptionFrontend.businessYourIncomeSource()

      Then("Should return a OK with the income source page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("your-income-source.title") + serviceNameGovUk)
      )
    }

    "redirect to task list" when {
      "there are no options left" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, OK, Json.toJson(testTooManyBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.businessIncomeSource()

        Then("Should return 303 with the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(businessRoutes.TaskListController.show().url)
        )
      }
    }
  }



}
