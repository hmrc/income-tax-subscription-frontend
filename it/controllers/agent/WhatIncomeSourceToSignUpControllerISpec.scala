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

class WhatIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {
  private val serviceNameGovUk = "Use software to report your client’s Income Tax - GOV.UK"
  "GET /report-quarterly/income-and-expenses/sign-up/client/income-source" should {
    "return OK" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

      When(s"GET ${routes.WhatIncomeSourceToSignUpController.show().url} is called")
      val res = IncomeTaxSubscriptionFrontend.businessIncomeSource()

      Then("Should return OK with the income source page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("agent.what_income_source_to_sign_up.heading")} - $serviceNameGovUk"
        )
      )
    }

    "redirect to task list" when {
      "there are no options left" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, OK, Json.toJson(testTooManyBusinesses))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When(s"GET ${routes.WhatIncomeSourceToSignUpController.show().url} is called")
        val res = IncomeTaxSubscriptionFrontend.businessIncomeSource()

        Then("Should return 303 with the task list page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.TaskListController.show().url)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/income-source" should {
    "redirect to the start of the self employment journey" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

      When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(SelfEmployed))

      Then(s"Should return $SEE_OTHER with a redirect location of the start of the self employment journey")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl)
      )
    }

    "redirect to the UK property start date page" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

      When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(UkProperty))

      Then(s"Should return $SEE_OTHER with a redirect location of property commencement date")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(propertyStartDateURI)
      )
    }

    "redirect to the overseas property start date page" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      And("Foreign property feature switch is enabled")
      enable(ForeignPropertyFeature)

      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

      When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(OverseasProperty))

      Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(overseasPropertyStartDateURI)
      )
    }

    "return a BAD_REQUEST (400)" when {
      "no input is selected" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("Foreign property feature switch is enabled")
        enable(ForeignPropertyFeature)

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(None)

        Then(s"Should return $BAD_REQUEST with the income source page plus error")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("what_income_source_to_sign_up.title")} - $serviceNameGovUk")
        )
      }

      "self employment is selected but the user already has 50 self employment businesses" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("Foreign property feature switch is enabled")
        enable(ForeignPropertyFeature)

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          id = SubscriptionDataKeys.BusinessesKey,
          responseStatus = OK,
          responseBody = Json.toJson(Seq.fill(appConfig.maxSelfEmployments)(
            SelfEmploymentData("testId")
          ))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(SelfEmployed))

        Then(s"Should return $BAD_REQUEST with the income source page plus error")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("what_income_source_to_sign_up.title")} - $serviceNameGovUk")
        )
      }

      "uk property is selected but the user already has started uk property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("Foreign property feature switch is enabled")
        enable(ForeignPropertyFeature)

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          id = SubscriptionDataKeys.Property,
          responseStatus = OK,
          responseBody = Json.toJson(PropertyModel(accountingMethod = Some(Cash)))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(UkProperty))

        Then(s"Should return $BAD_REQUEST with the income source page plus error")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("what_income_source_to_sign_up.title")} - $serviceNameGovUk")
        )
      }

      "overseas property is started but the user already has started overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("Foreign property feature switch is enabled")
        enable(ForeignPropertyFeature)

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          id = SubscriptionDataKeys.OverseasProperty,
          responseStatus = OK,
          responseBody = Json.toJson(OverseasPropertyModel(accountingMethod = Some(Cash)))
        )

        When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(OverseasProperty))

        Then(s"Should return $BAD_REQUEST with the income source page plus error")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("what_income_source_to_sign_up.title")} - $serviceNameGovUk")
        )
      }

      "overseas property but the overseas property feature switch is disabled" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("Foreign property feature switch is disabled")
        disable(ForeignPropertyFeature)

        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.BusinessesKey, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)

        When(s"POST ${routes.WhatIncomeSourceToSignUpController.submit().url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(OverseasProperty))

        Then(s"Should return $BAD_REQUEST with the income source page plus error")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle(s"Error: ${messages("what_income_source_to_sign_up.title")} - $serviceNameGovUk")
        )
      }
    }
  }
}
