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

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class AlreadySignedUpControllerISpec extends ComponentSpecBase {


  "GET /claim-enrolment/already-signed-up " should {
    "return the already signed up page" in {

      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()
      When("GET /claim-enrolment/already-signed-up  is called")
      val res = IncomeTaxSubscriptionFrontend.alreadySignedUp()
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      Then("Should return a OK with the already Signed Up page")
      res must have(
        httpStatus(OK),
        pageTitle(messages("claim-enrolment.claimAlreadySignedUp.title") + serviceNameGovUk)
      )
    }
  }

}
