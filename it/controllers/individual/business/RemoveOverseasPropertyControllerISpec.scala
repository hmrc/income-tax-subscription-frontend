
package controllers.individual.business

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.taskListURI
import helpers.servicemocks.AuthStub
import models.{No, Yes}
import play.api.http.Status._
import utilities.SubscriptionDataKeys.OverseasProperty

class RemoveOverseasPropertyControllerISpec extends ComponentSpecBase {

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.RemoveOverseasPropertyController.show.url}" should {
    s"return $OK" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When(s"GET ${routes.RemoveOverseasPropertyController.show.url}")
      val res = IncomeTaxSubscriptionFrontend.getRemoveOverseasProperty()

      Then("Should return a OK with the remove overseas property business page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("remove-overseas-property-business.heading") + serviceNameGovUk)
      )
    }
  }

  s"POST ${routes.RemoveOverseasPropertyController.submit.url}" should {
    s"return $SEE_OTHER" when {
      "the user selects to delete their overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(Yes))

        Then("Should return a NOT_FOUND page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
        )

        IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
      }
      "the user selects to not delete their overseas property" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(No))

        Then("Should return a NOT_FOUND page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(taskListURI)
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

        Then("Should return a NOT_FOUND page")
        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("remove-overseas-property-business.heading") + serviceNameGovUk)
        )
      }
    }
  }
}