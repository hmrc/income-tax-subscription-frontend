
package controllers.usermatching

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class AffinityGroupErrorControllerISpec extends ComponentSpecBase {
  "GET /error/affinity-group" should {
    "show the no matching user page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/user-details is called")
      val res = IncomeTaxSubscriptionFrontend.showAffinityGroupError()
      val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
      Then("Should return a OK with the no matching user page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("affinity-group-error.title") + serviceNameGovUk)
      )
    }
  }
}
