
package controllers.individual.business

import java.time.LocalDate

import helpers.IntegrationTestConstants.{accountingMethodPropertyURI, cannotSignUpURI, checkYourAnswersURI}
import helpers.IntegrationTestModels.{keystoreData, testPropertyCommencementDate}
import helpers.servicemocks.{AuthStub, KeystoreStub}
import helpers.{ComponentSpecBase, IntegrationTestModels}
import models.DateModel
import models.individual.business.PropertyCommencementDateModel
import models.individual.incomesource.IncomeSourceModel
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import utilities.CacheConstants

class PropertyCommencementDateISpec extends ComponentSpecBase {

  "GET /report-quarterly/income-and-expenses/sign-up/business/property-commencement-date" when {

    "keystore returns all data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystoreBothPost()

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyCommencementDate()

        Then("Should return a OK with the property commencement page with populated commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title")),
          dateField("startDate", testPropertyCommencementDate.startDate)
        )
      }
    }

    "keystore returns no data" should {
      "show the property commencement date page" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true)
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(individualIncomeSource = Some(incomeSourceModel)))

        When("GET /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.propertyCommencementDate()

        Then("Should return a OK with the property commencement date page with no commencement date")
        res should have(
          httpStatus(OK),
          pageTitle(messages("business.property.name.title"))
        )
      }
    }

  }

  "POST /report-quarterly/income-and-expenses/sign-up/property-commencement-date" when {
    "not in edit mode" when {
      "enter commencement date" should {
        "redirect to the accounting method page" in {
          val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubKeystoreData(keystoreData())
          KeystoreStub.stubKeystoreSave(CacheConstants.PropertyCommencementDate, userInput)

          When("POST /property-commencement-date is called")
          val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, Some(userInput))

          Then("Should return a SEE_OTHER with a redirect location of property accounting method page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(accountingMethodPropertyURI)
          )
        }
      }

      "do not enter commencement date" in {
        Given("I setup the Wiremock stubs")
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true)
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(individualIncomeSource = Some(incomeSourceModel)))
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyCommencementDate, "")

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, None)

        Then("Should return a BAD_REQUEST and display an error box on screen without redirecting")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

      "select commencement date within 12 months" in {
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testInvalidCommencementDate
        val incomeSourceModel: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(keystoreData(individualIncomeSource = Some(incomeSourceModel)))
        KeystoreStub.stubKeystoreSave(CacheConstants.BusinessName, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = false, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of cannot sign up")
        res should have(
          httpStatus(BAD_REQUEST),
          errorDisplayed()
        )
      }

    }

    "in edit mode" should {
      "simulate not changing commencement date when calling page from Check Your Answers" in {
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyCommencementDate, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

      "simulate changing commencement date when calling page from Check Your Answers" in {
        val keystoreCommencementDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(2))
        val keystorePropertyCommencementDate = PropertyCommencementDateModel(keystoreCommencementDate)
        val userInput: PropertyCommencementDateModel = IntegrationTestModels.testPropertyCommencementDate

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            propertyCommencementDate = Some(keystorePropertyCommencementDate)
          )
        )
        KeystoreStub.stubKeystoreSave(CacheConstants.PropertyCommencementDate, userInput)

        When("POST /property-commencement-date is called")
        val res = IncomeTaxSubscriptionFrontend.submitpropertyCommencementDate(inEditMode = true, Some(userInput))

        Then("Should return a SEE_OTHER with a redirect location of check your answers")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersURI)
        )
      }

    }

  }
}
