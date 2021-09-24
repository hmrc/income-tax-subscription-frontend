/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.individual.claimenrolment

import config.featureswitch.FeatureSwitch.ClaimEnrolment
import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class NotSubscribedControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(ClaimEnrolment)
    super.beforeEach()
  }

  "GET /claim-enrolment/not-subscribed" should {
    "return the not subscribed page" when {
      "the claim enrolment feature switch is enabled" in {
        enable(ClaimEnrolment)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/not-subscribed is called")
        val res = IncomeTaxSubscriptionFrontend.notSubscribed()
        val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
        Then("Should return a OK with the not subscribed page")
        res should have(
          httpStatus(OK),
          pageTitle(messages("claim-enrolment.not-subscribed.heading") + serviceNameGovUk)
        )
      }
    }
    "return a not found page" when {
      "the claim enrolment feature switch is disabled" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        When("GET /claim-enrolment/confirmation is called")
        val res = IncomeTaxSubscriptionFrontend.notSubscribed()

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

}