
package controllers.agent.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants.checkYourAnswersURI
import helpers.agent.IntegrationTestModels.subscriptionData
import helpers.agent.servicemocks.AuthStub
import models.{Accruals, Cash}
import models.common.OverseasAccountingMethodPropertyModel
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class OverseasPropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-accounting-method" when {

    "Subscription details returns pre-populated data" should {
      "show the foreign property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionData()

        When("GET /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("agent.overseas.property.accounting_method.radio.cash"))

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodOverseasProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "Subscription details returns with no pre-populated data" should {
      "show the foreign property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(None))

        When("GET /business/overseas-property-accounting-method is called")

        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"

        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "foreignPropertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }
  }

  "POST /business/accounting-method-property" when {
    "not in Edit Mode" should {
      "select the Cash radio button on the Overseas Property Accounting Method page" in {
        val userInput = OverseasAccountingMethodPropertyModel(Cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "select the Accruals radio button on the Overseas Property Accounting Method page" in {
        val userInput = OverseasAccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "not select a radio button on the Overseas Property Accounting Method page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, "")

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }
    }

    "in Edit Mode" should {
      "changing to the Accruals radio button on the overseas property accounting method page" in {
        val SubscriptionDetailsAccountingMethodProperty = OverseasAccountingMethodPropertyModel(Cash)
        val userInput = OverseasAccountingMethodPropertyModel(Accruals)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(overseasPropertyAccountingMethod = Some(SubscriptionDetailsAccountingMethodProperty))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "not changing the radio button on the overseas property accounting method page" in {
        val SubscriptionDetailsAccountingMethodProperty = OverseasAccountingMethodPropertyModel(Cash)
        val userInput = OverseasAccountingMethodPropertyModel(Cash)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(overseasPropertyAccountingMethod = Some(SubscriptionDetailsAccountingMethodProperty))
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

        When("POST /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyAccountingMethod(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }
    }
  }
}
