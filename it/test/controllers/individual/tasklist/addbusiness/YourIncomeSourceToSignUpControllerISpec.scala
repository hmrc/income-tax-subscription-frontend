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

package controllers.individual.tasklist.addbusiness

import config.featureswitch.FeatureSwitch.{ForeignProperty => ForeignPropertyFeature}
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI.taskListURI
import helpers.IntegrationTestModels.{testAccountingMethod, testBusiness, testBusinesses, testFullOverseasPropertyModel, testFullPropertyModel}
import helpers.servicemocks.AuthStub
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

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.heading") + serviceNameGovUk)
        )
      }
      "there are multiple income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT, testBusinesses.getOrElse(Seq.empty), Some(testAccountingMethod.accountingMethod))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.manage.heading") + serviceNameGovUk)
        )
      }
    }
  }

  s"POST ${routes.YourIncomeSourceToSignUpController.submit.url}" when {
    "the user has complete businesses" should {
      "redirect to the task list page and save the income source section completion" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
          OK,
          Seq(testBusiness("12345", confirmed = true)),
          Some(testAccountingMethod.accountingMethod)
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true)

        When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        Then("Should return a SEE_OTHER to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(1))
      }
    }
    "the user has a mixture of complete and incomplete businesses" should {
      "redirect to the task list page and not save an income source section completion" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
          OK,
          Seq(testBusiness("12345", confirmed = true), testBusiness("54321")),
          Some(testAccountingMethod.accountingMethod)
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        Then("Should return a SEE_OTHER to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
      }
    }
    "the user has no businesses" should {
      "redirect to the task list page and not save an income source section completion" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        Then("Should return a SEE_OTHER to the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
      }
    }
  }
}
