
package controllers.usermatching

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import play.api.i18n.Messages

class AffinityGroupErrorControllerISpec extends ComponentSpecBase {
  "GET /error/affinity-group" should {
    "show the no matching user page" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /error/user-details is called")
      val res = IncomeTaxSubscriptionFrontend.showAffinityGroupError()

      Then("Should return a OK with the no matching user page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("affinity-group-error.title"))
      )
    }
  }
}
