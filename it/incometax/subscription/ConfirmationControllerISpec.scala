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

package incometax.subscription

import core.services.CacheConstants._
import core.utils.JsonUtils._
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.testMTDID
import helpers.IntegrationTestModels.testRentUkProperty_property_only
import helpers.servicemocks.{AuthStub, KeystoreStub}
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json.JsString

class ConfirmationControllerISpec extends ComponentSpecBase {

  "GET /confirmation" should {
    "return the confirmation page when the user is enrolled" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      KeystoreStub.stubKeystoreData(Map(
        RentUkProperty -> testRentUkProperty_property_only,
        MtditId -> JsString(testMTDID)
      ))

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.confirmation()

      Then("Should return a OK with the confirmation page")
      res should have(
        httpStatus(OK),
        pageTitle(Messages("confirmation.title"))
      )
    }

    "return a NOT_FOUND when the user is not enrolled" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.confirmation()

      Then("Should return a NOT_FOUND status")
      res should have(
        httpStatus(NOT_FOUND))
    }
  }

}
