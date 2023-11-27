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
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.IntegrationTestModels.{testBusinesses, testFullOverseasPropertyModel, testFullPropertyModel}
import helpers.servicemocks.AuthStub
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys

class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignPropertyFeature)
  }

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.YourIncomeSourceToSignUpController.show.url}" should {
    "return OK" when {
      "there are no income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.title") + serviceNameGovUk)
        )
      }
      "there are multiple income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, OK, Json.toJson(testBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.title") + serviceNameGovUk)
        )
      }
    }
  }

  s"POST ${routes.YourIncomeSourceToSignUpController.submit.url}" should {
    "redirect to the task list page when you select yes" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubSaveIncomeSourceConfirmation(true)

      When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources(Some(Yes))

      Then("Should redirect to the task list page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.taskListURI)
      )
    }

    "redirect to the task list page when you select no" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources(Some(No))

      Then("Should redirect to the task list page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.taskListURI)
      )
    }

    "return a BAD Request when no data is submitted" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, OK, Json.toJson(testBusinesses))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

      When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources(None)

      Then("Should redirect to the task list page")
      res must have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }
  }


}
