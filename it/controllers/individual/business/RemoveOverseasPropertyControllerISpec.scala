
package controllers.individual.business

import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.taskListURI
import helpers.servicemocks.AuthStub
import models.{No, Yes}
import play.api.http.Status._
import utilities.SubscriptionDataKeys.OverseasProperty

class RemoveOverseasPropertyControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SaveAndRetrieve)
  }

  val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"

  s"GET ${routes.RemoveOverseasPropertyController.show.url}" should {
    s"return $OK" when {
      "the save and retrieve feature switch is enabled" in {
        enable(SaveAndRetrieve)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.show.url}")
        val res = IncomeTaxSubscriptionFrontend.getRemoveOverseasProperty()

        Then("Should return a OK with the remove overseas property business page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("remove-overseas-property-business.heading") + serviceNameGovUk)
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the save and retrieve feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.show.url}")
        val res = IncomeTaxSubscriptionFrontend.getRemoveOverseasProperty()

        Then("Should return a NOT_FOUND page")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  s"POST ${routes.RemoveOverseasPropertyController.submit.url}" when {
    "the save and retrieve feature switch is enabled" should {
      s"return $SEE_OTHER" when {
        "the user selects to delete their overseas property" in {
          enable(SaveAndRetrieve)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(OverseasProperty)

          When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(Yes))

          Then("Should return a NOT_FOUND page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(1))
        }
        "the user selects to not delete their overseas property" in {
          enable(SaveAndRetrieve)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(Some(No))

          Then("Should return a NOT_FOUND page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(taskListURI)
          )

          IncomeTaxSubscriptionConnectorStub.verifyDeleteSubscriptionDetails(OverseasProperty, Some(0))
        }
      }
      s"return $BAD_REQUEST" when {
        "the user does not select an option" in {
          enable(SaveAndRetrieve)

          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()

          When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
          val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(None)

          Then("Should return a NOT_FOUND page")
          res should have(
            httpStatus(BAD_REQUEST),
            pageTitle("Error: " + messages("remove-overseas-property-business.heading") + serviceNameGovUk)
          )
        }
      }
    }
    "the save and retrieve feature switch is disabled" should {
      s"return $NOT_FOUND" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        When(s"GET ${routes.RemoveOverseasPropertyController.submit.url}")
        val res = IncomeTaxSubscriptionFrontend.submitRemoveOverseasProperty()(None)

        Then("Should return a NOT_FOUND page")
        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }


}