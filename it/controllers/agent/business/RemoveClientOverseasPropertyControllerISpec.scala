package controllers.agent.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.agent.ComponentSpecBase
import helpers.agent.IntegrationTestConstants._
import helpers.agent.IntegrationTestModels.overseasPropertySubscriptionData
import helpers.agent.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty

class ClientRemoveOverseasPropertyControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" when {

    "return OK" when {
      "save and retrieve feature switch is enabled" in {
        Given("I setup the Wiremock stubs")
        enable(SaveAndRetrieve)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))

        When("GET client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the client remove Overseas property confirmation page displaying")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.remove-overseas-property-business.heading") + serviceNameGovUk)
        )
      }
    }

    "return NOT_FOUND" when {
      "save and retrieve is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))

        When("GET client/business/remove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.getRemoveClientOverseasProperty

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/remove-overseas-property-business" when {

    "save and retrieve is enabled" should {
      "redirect to the client task list page" when {
        "the user submits the 'yes' answer" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))
          IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)
          enable(SaveAndRetrieve)

          When("POST client/business/remove-overseas-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

          Then("Should return a SEE_OTHER with a redirect location of client task list page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
        }

        "the user submits the 'no' answer" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))
          enable(SaveAndRetrieve)

          When("POST client/business/remove-overseas-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("No")))

          Then("Should return a SEE_OTHER with a redirect location of client task list page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
        }
      }
      "return a BAD_REQUEST" when {
        "no option was selected on the client remove Overseas property page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))
          enable(SaveAndRetrieve)

          When("POST /business/remove-overseas-property-business is called")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("")))

          Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
          res should have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
        }
      }
    }

    "save and retrieve is disabled" should {
      "return NOT_FOUND" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(overseasPropertySubscriptionData))

        When("POST business/removeremove-overseas-property-business is called")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveClientOverseasProperty(Map("yes-no" -> Seq("Yes")))

        Then("Should return NOT_FOUND")
        res should have(
          httpStatus(NOT_FOUND)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
      }
    }

  }
}