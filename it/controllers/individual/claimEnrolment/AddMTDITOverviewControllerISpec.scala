package controllers.individual.claimEnrolment

import config.featureswitch.FeatureSwitch.ClaimEnrolment
import config.featureswitch.FeatureSwitching
import controllers.Assets.SEE_OTHER
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.AddMTDITOverviewURI
import helpers.servicemocks.AuthStub
import play.api.http.Status.{NOT_FOUND, OK}

class AddMTDITOverviewControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  "GET /claim-enrolment/overview" should {
    "show the AddMTDITOverview page" in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      enable(ClaimEnrolment)

      When("GET /claim-enrolment/overview is called")
      val res = IncomeTaxSubscriptionFrontend.addMTDITOverview()
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      Then("Should return a OK with the AddMTDITOverview page")
      res should have(
        httpStatus(OK),
        pageTitle(messages("mtdit-overview.heading") + serviceNameGovUk)
      )
    }
  }


  "GET /claim-enrolment/overview" should {
    "not show the AddMTDITOverview page when the ClaimEnrolment Feature switch is off" in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /claim-enrolment/overview is called")
      val res = IncomeTaxSubscriptionFrontend.addMTDITOverview()
      Then("Should return a OK with the AddMTDITOverview page")
      res should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404")
      )
    }
  }

  "POST/claim-enrolment/overview" should {
    "Redirect to AddMTDITOverview page" in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      enable(ClaimEnrolment)

      When("POST /claim-enrolment/overview is called")
      val res = IncomeTaxSubscriptionFrontend.submitAddMTDITOverview()

      Then("Should return a SEE_OTHER with a redirect location of AddMTDITOverview page")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(AddMTDITOverviewURI)
      )
    }
}

}
