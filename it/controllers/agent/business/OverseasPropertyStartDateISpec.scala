
package controllers.agent.business

import java.time.LocalDate

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestModels.{subscriptionData, testPropertyStartDate}
import helpers.IntegrationTestModels
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyStartDateModel}
import play.api.http.Status._
import utilities.SubscriptionDataKeys
import helpers.agent.IntegrationTestConstants._

class OverseasPropertyStartDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" when {

    "Subscription Details returns all data" should {
      "show the Overseas property Start date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionGet()

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the Overseas property Start page with populated start date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.name.heading") + serviceNameGovUk),
          dateField("startDate", testPropertyStartDate.startDate)
        )
      }
    }

    "Subscription Details returns no data" should {
      "show the Overseas property Start date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyStartDate()
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        Then("Should return a OK with the Overseas property Start date page with no start date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("agent.overseas.property.name.heading") + serviceNameGovUk)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/client/business/overseas-property-start-date" when {
    "not in edit mode" when {
      "enter start date" should {
        "redirect to the Overseas property accounting method page" in {
          val userInput: OverseasPropertyStartDateModel = IntegrationTestModels.testOverseasPropertyStartDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.subscriptionId, userInput)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of Overseas property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(overseasPropertyAccountingMethod)
          )
        }
      }

      "do not enter start date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyStartDate, "")

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select start date within 12 months" in {
        val userInput: OverseasPropertyStartDateModel = IntegrationTestModels.testInvalidOverseasPropertyStartDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false,
          foreignProperty = true)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.BusinessName, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing start date when calling page from Check Your Answers" in {
        val userInput: OverseasPropertyStartDateModel = IntegrationTestModels.testOverseasPropertyStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(overseasPropertyStartDate = Some(userInput)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyStartDate, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing start date when calling page from Check Your Answers" in {
        val subscriptionDetailsStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val subscriptionDetailsOverseasPropertyStartDate = OverseasPropertyStartDateModel(subscriptionDetailsStartDate)
        val userInput: OverseasPropertyStartDateModel = IntegrationTestModels.testOverseasPropertyStartDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            overseasPropertyStartDate = Some(subscriptionDetailsOverseasPropertyStartDate)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyStartDate, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitOverseasPropertyStartDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
