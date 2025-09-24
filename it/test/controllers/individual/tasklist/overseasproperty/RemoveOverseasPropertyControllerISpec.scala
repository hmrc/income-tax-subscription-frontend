/*
 * Copyright 2023 HM Revenue & Customs
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
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys
import utilities.SubscriptionDataKeys.OverseasProperty

class RemoveOverseasPropertyControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.RemoveOverseasPropertyController.show.url}" should {
    s"return $OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK,
        Json.toJson(OverseasPropertyModel()))

      When(s"GET ${routes.RemoveOverseasPropertyController.show.url}")
      val res = IncomeTaxSubscriptionFrontend.getRemoveOverseasProperty()

      Then("Should return a OK with the remove overseas property business page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("remove-overseas-property-business.heading") + serviceNameGovUk)
      )
    }

    "redirect to Business Already removed page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT,
        Json.toJson(OverseasPropertyModel(None)))

      When("GET client/business/remove-uk-property-business is called")
      val res = IncomeTaxSubscriptionFrontend.getRemoveOverseasProperty()
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.individual.tasklist.addbusiness.routes.BusinessAlreadyRemovedController.show().url)
      )
    }
  }

  s"POST ${routes.RemoveOverseasPropertyController.submit.url}" should {
    s"return $SEE_OTHER" when {
      "the user selects to delete their overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(Yes))

        Then("Should return a SEE_OTHER status")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation, Some(1))
      }

      "the user selects to not delete their overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(No))

        Then("Should return a SEE_OTHER status")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(IndividualURI.yourIncomeSourcesURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

    s"return $BAD_REQUEST" when {
      "the user does not select an option" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(None)

        Then("Should return a BAD_REQUEST status")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("remove-overseas-property-business.heading") + serviceNameGovUk)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR" when {
      "cannot remove the overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(OverseasProperty)

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(Yes))

        Then("Should return an INTERNAL_SERVER_ERROR status")
        res must have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}