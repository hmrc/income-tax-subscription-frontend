
package controllers.individual.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.testFullOverseasPropertyModel
import helpers.servicemocks.AuthStub
import models._
import models.common.OverseasPropertyModel
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.OverseasProperty


class OverseasPropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(SaveAndRetrieve)
    super.beforeEach()
  }

  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-accounting-method" when {

    "Subscription details returns pre-populated data" should {
      "show the foreign property accounting method page with an option selected" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

        When("GET /business/overseas-property-accounting-method is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyAccountingMethod()

        val expectedText = removeHtmlMarkup(messages("summary.income_type.cash"))
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
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

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
    "select the Cash radio button on the foreign property accounting method page" when {
      "not in edit mode" when {
        "save and retrieve is enabled" should {
          "redirect to overseas property check your answers page" in {
            val userInput = Cash
            val expected = OverseasPropertyModel(accountingMethod = Some(userInput))
            enable(SaveAndRetrieve)
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

            When("POST /business/overseas-property-accounting-method is called")
            val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = false, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of overseas check your answers")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(overseasPropertyCYAURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
          }
        }

        "save and retrieve is disabled" should {
          "redirect to final check your answers page" in {
            val userInput = Cash
            val expected = OverseasPropertyModel(accountingMethod = Some(userInput))
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

            When("POST /business/overseas-property-accounting-method is called")
            val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = false, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(checkYourAnswersURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
          }
        }
      }

      "in edit mode" when {
        "save and retrieve is enabled" should {
          "redirect to overseas property check your answers page" in {
            val userInput = Cash
            val expected = OverseasPropertyModel(accountingMethod = Some(userInput))
            enable(SaveAndRetrieve)
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

            When("POST /business/overseas-property-accounting-method is called")
            val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of overseas check your answers")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(overseasPropertyCYAURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
          }
        }

        "save and retrieve is disabled" should {
          "redirect to final check your answers page" in {
            val userInput = Cash
            val expected = OverseasPropertyModel(accountingMethod = Some(userInput))
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, OK, Json.toJson(OverseasPropertyModel()))
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

            When("POST /business/overseas-property-accounting-method is called")
            val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyAccountingMethod(inEditMode = true, Some(userInput))

            Then("Should return a SEE_OTHER with a redirect location of check your answers")
            res should have(
              httpStatus(SEE_OTHER),
              redirectURI(checkYourAnswersURI)
            )

            IncomeTaxSubscriptionConnectorStub.verifySaveOverseasProperty(expected, Some(1))
          }
        }
      }

    }

    "select the Accruals radio button on the foreign property accounting method page" in {
      val userInput = Accruals
      val expected = OverseasPropertyModel(accountingMethod = Some(userInput))

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionBothPost()
      IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(OverseasProperty, NO_CONTENT)
      IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(expected)

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