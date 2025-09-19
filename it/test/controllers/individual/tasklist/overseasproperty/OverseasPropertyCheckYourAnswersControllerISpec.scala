/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.individual.tasklist.overseasproperty

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import connectors.stubs.IncomeTaxSubscriptionConnectorStub.subscriptionUri
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.agent.WiremockHelper.verifyPost
import helpers.servicemocks.AuthStub
import models.DateModel
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class OverseasPropertyCheckYourAnswersControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-check-your-answers" should {
    "return OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))

      When("GET business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers()

      Then("Should return OK with the overseas property CYA page")
      res must have(
        httpStatus(OK),
        pageTitle(
          s"${messages("business.check-your-answers.content.overseas-property.title")} - Use software to send Income Tax updates - GOV.UK"
        )
      )
    }

    "return INTERNAL_SERVER_ERROR" when {
      "overseas property data cannot be retrieved" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

        When("GET business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.getOverseasPropertyCheckYourAnswers()

        Then("Should return INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/overseas-property-check-your-answers" should {
    "save when startDateBeforeLimit is true" in {
      val testProperty = OverseasPropertyModel(startDateBeforeLimit = Some(true), startDate = None)
      val expectedProperty = testProperty.copy(confirmed = true)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        OverseasProperty, OK, Json.toJson(testProperty)
      )
      IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expectedProperty)
      IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

      When("POST business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of your income sources page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.yourIncomeSourcesURI)
      )
      IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expectedProperty, Some(1))
    }

    "save when startDateBeforeLimit is false but the start Date defined" in {
      val testProperty = OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = Some(DateModel("10", "11", "2021")))
      val expectedProperty = testProperty.copy(confirmed = true)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        OverseasProperty, OK, Json.toJson(testProperty)
      )
      IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expectedProperty)
      IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

      When("POST business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of your income sources page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.yourIncomeSourcesURI)
      )
      IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expectedProperty, Some(1))
    }

    "not save the property answers when foreign property is incomplete" in {
      val testProperty = OverseasPropertyModel(startDateBeforeLimit = Some(false), startDate = None)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        OverseasProperty,
        OK,
        Json.toJson(testProperty)
      )

      When("POST business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

      Then("Should return a SEE_OTHER with a redirect location of your income sources page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.yourIncomeSourcesURI)
      )

      verifyPost(subscriptionUri(OverseasProperty), count = Some(0))
    }
  }


  "return INTERNAL_SERVER_ERROR" when {
    "overseas property data cannot be retrieved" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)

      When("POST business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

      Then("Should return a INTERNAL_SERVER_ERROR")
      res must have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "overseas property data cannot be confirmed" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
        OverseasProperty,
        OK,
        Json.toJson(OverseasPropertyModel(startDate = Some(DateModel("10", "11", "2021"))))
      )
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)

      When("POST business/overseas-property-check-your-answers is called")
      val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

      Then("Should return a INTERNAL_SERVER_ERROR")
      res must have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "feature switch is enabled" should {
      "overseas property data cannot be confirmed" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
          OverseasProperty,
          OK,
          Json.toJson(OverseasPropertyModel(startDate = Some(DateModel("10", "11", "2021"))))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(OverseasProperty)

        When("POST business/overseas-property-check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyCheckYourAnswers()

        Then("Should return a INTERNAL_SERVER_ERROR")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
