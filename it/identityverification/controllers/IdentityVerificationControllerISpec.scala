/*
 * Copyright 2017 HM Revenue & Customs
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

package identityverification.controllers

import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class IdentityVerificationControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up/iv" should {
    "redirect to the IV service" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /iv is called")
      val res = IncomeTaxSubscriptionFrontend.iv()

      Then("Should redirect to Identity Verification")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(appConfig.identityVerificationURL)
      )
    }
  }
}
