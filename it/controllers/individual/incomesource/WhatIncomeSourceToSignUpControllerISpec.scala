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

import config.featureswitch.FeatureSwitch.{SaveAndRetrieve, ForeignProperty => ForeignPropertyFeature}
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{overseasPropertyStartDateURI, propertyStartDateURI}
import helpers.servicemocks.AuthStub
import models.common._
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class WhatIncomeSourceToSignUpControllerISpec extends ComponentSpecBase with FeatureSwitching {
  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  "GET /report-quarterly/income-and-expenses/sign-up/details/income-source" should {
    "return OK" when {
      "the save & retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is enabled")
        enable(SaveAndRetrieve)

        When("GET /details/income-source is called")
        val res = IncomeTaxSubscriptionFrontend.businessIncomeSource()

        Then("Should return a OK with the income source page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("what_income_source_to_sign_up.title") + serviceNameGovUk)
        )
      }
    }

    "return NOT_FOUND" when {
      "the save & retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        And("save & retrieve feature switch is disabled")
        disable(SaveAndRetrieve)

        When("GET /details/income-source is called")
        val res = IncomeTaxSubscriptionFrontend.businessIncomeSource()

        Then("Should return NOT FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/details/income-source" should {
    "redirect to the start of the self employment journey" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /details/income-receive is called")
      val userInput = BusinessIncomeSourceModel(SelfEmployed)
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(userInput))

      Then(s"Should return $SEE_OTHER with a redirect location of the start of the self employment journey")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl)
      )
    }

    "redirect to the UK property start date page" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("POST /details/income-receive is called")
      val userInput = BusinessIncomeSourceModel(UkProperty)
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(userInput))

      Then(s"Should return $SEE_OTHER with a redirect location of property commencement date")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(propertyStartDateURI)
      )
    }

    "redirect to the overseas property start date page" in {
      Given("I setup the wiremock stubs")
      AuthStub.stubAuthSuccess()
      And("Foreign property feature switch is enabled")
      enable(ForeignPropertyFeature)

      When("POST /details/income-receive is called")
      val userInput = BusinessIncomeSourceModel(ForeignProperty)
      val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(userInput))

      Then(s"Should return $SEE_OTHER with a redirect location of overseas property commencement date")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(overseasPropertyStartDateURI)
      )
    }

    "return INTERNAL_SERVER_ERROR" when {
      "the user selects overseas property and the foreign property feature switch is disabled" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(
          SubscriptionDataKeys.IncomeSource,
          IncomeSourceModel(selfEmployment = false, ukProperty = false, foreignProperty = true)
        )
        And("Foreign property feature switch is disabled")
        disable(ForeignPropertyFeature)

        When("POST /details/income-receive is called")
        val userInput = BusinessIncomeSourceModel(ForeignProperty)
        val res = IncomeTaxSubscriptionFrontend.submitBusinessIncomeSource(Some(userInput))

        Then("Should return INTERNAL_SERVER_ERROR")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
