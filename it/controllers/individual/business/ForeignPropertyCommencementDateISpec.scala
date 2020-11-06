
package controllers.individual.business

import java.time.LocalDate

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels.{subscriptionData, testPropertyCommencementDate}
import helpers.servicemocks.AuthStub
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.common.{IncomeSourceModel, OverseasPropertyCommencementDateModel}
import play.api.http.Status._
import utilities.SubscriptionDataKeys

class ForeignPropertyCommencementDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/overseas-property-start-date" when {

    "Subscription Details returns all data" should {
      "show the foreign property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubFullSubscriptionGet()

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyCommencementDate()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the foreign property commencement page with populated commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("foreign.property.name.title") + serviceNameGovUk),
          dateField("startDate", testPropertyCommencementDate.startDate)
        )
      }
    }

    "Subscription Details returns no data" should {
      "show the foreign property commencement date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = true)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))

        When("GET /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.overseasPropertyCommencementDate()
        val serviceNameGovUk = " - Report your income and expenses quarterly - GOV.UK"
        Then("Should return a OK with the foreign property commencement date page with no commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("foreign.property.name.title") + serviceNameGovUk)
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/business/overseas-property-start-date" when {
    "not in edit mode" when {
      "enter commencement date" should {
        "redirect to the foreign property accounting method page" in {
          val userInput: OverseasPropertyCommencementDateModel = IntegrationTestModels.testForeignPropertyCommencementDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData())
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.subscriptionId, userInput)

          When("POST /overseas-property-start-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyCommencementDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of foreign property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodForeignPropertyURI)
          )
        }
      }

      "do not enter commencement date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true,
          foreignProperty = false)
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyCommencementDate, "")

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyCommencementDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select commencement date within 12 months" in {
        val userInput: OverseasPropertyCommencementDateModel = IntegrationTestModels.testInvalidForeignPropertyCommencementDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false,
          foreignProperty = true)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(incomeSource = Some(incomeSourceModel)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.BusinessName, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyCommencementDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing commencement date when calling page from Check Your Answers" in {
        val userInput: OverseasPropertyCommencementDateModel = IntegrationTestModels.testForeignPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(subscriptionData(overseasPropertyCommencementDate = Some(userInput)))
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyCommencementDate, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing commencement date when calling page from Check Your Answers" in {
        val subscriptionDetailsCommencementDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val subscriptionDetailsForeignPropertyCommencementDate = OverseasPropertyCommencementDateModel(subscriptionDetailsCommencementDate)
        val userInput: OverseasPropertyCommencementDateModel = IntegrationTestModels.testForeignPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(
          subscriptionData(
            overseasPropertyCommencementDate = Some(subscriptionDetailsForeignPropertyCommencementDate)
          )
        )
        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SubscriptionDataKeys.OverseasPropertyCommencementDate, userInput)

        When("POST /overseas-property-start-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitForeignPropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
