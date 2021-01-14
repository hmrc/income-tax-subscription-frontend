
package controllers.individual.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{subscriptionData, testAccountingMethodForeignProperty}
import helpers.servicemocks.AuthStub
import models._
import models.common.OverseasAccountingMethodPropertyModel
import play.api.http.Status._
import utilities.SubscriptionDataKeys


class OverseasPropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-accounting-method" when {

    "Subscription details returns pre-populated data" should {
      "show the foreign property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionPropertyPost()

        When("GET /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("overseas.property.accounting_method.cash"))
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "accountingMethodOverseasProperty", selectedRadioButton = Some(expectedText))
        )
      }
    }

    "Subscription details returns with no pre-populated data" should {
      "show the foreign property accounting method page without an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)))

        When("GET /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the foreign property accounting method page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("overseas.property.accounting_method.title") + serviceNameGovUk),
          radioButtonSet(id = "foreignPropertyAccountingMethod", selectedRadioButton = None)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/overseas-property-accounting-method" when {

    "select the Cash radio button on the foreign property accounting method page" in {
      val userInput = OverseasAccountingMethodPropertyModel(Cash)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

      When("POST /business/overseas-property-accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a SEE_OTHER with a redirect location of check your answers")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(checkYourAnswersURI)
      )
    }

    "select the Accruals radio button on the foreign property accounting method page" in {
      val userInput = OverseasAccountingMethodPropertyModel(Accruals)

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, userInput)

      When("POST /business/overseas-property-accounting-method is called")
      val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = false, Some(userInput))

      Then("Should return a SEE_OTHER with a redirect location of check your answers")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(checkYourAnswersURI)
      )
    }

    "not select an option on the foreign property accounting method page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubEmptySubscriptionData()
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyAccountingMethod, "")

      When("POST /business/overseas-property-accounting-method is called")

      val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = false, None)

      Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
      res should have(
        httpStatus(BAD_REQUEST),
        errorDisplayed()
      )
    }
  }

}