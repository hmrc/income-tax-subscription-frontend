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

package controllers.individual.subscription

import config.featureswitch.FeatureSwitch.{ReleaseFour, SPSEnabled}
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.testSubscriptionId
import helpers.IntegrationTestModels.testIncomeSourceIndivProperty
import helpers.WiremockHelper.verifyPost
import helpers.servicemocks.AuthStub
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys._

class ConfirmationControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    disable(ReleaseFour)
    disable(SPSEnabled)
    super.beforeEach()
  }

  "GET /confirmation" should {
    "return the confirmation page when the user is enrolled and confirm SPS preferences" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(Map(
        IncomeSource -> Json.toJson(testIncomeSourceIndivProperty),
        MtditId -> JsString(testSubscriptionId)
      ))

      When("GET /confirmation is called")
      val res = IncomeTaxSubscriptionFrontend.confirmation()
      val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
      Then("Should return a OK with the confirmation page")
      res should have(
        httpStatus(OK),
        pageTitle(messages("sign-up-complete.title") + serviceNameGovUk)
      )
    }

    "confirm SPS preferences" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(Map(
        IncomeSource -> Json.toJson(testIncomeSourceIndivProperty),
        MtditId -> JsString(testSubscriptionId)
      ))
      And("The SPS feature switch is enabled and sps entity is present in the session")
      enable(SPSEnabled)
      val sessionMap = Map("SPS-Entity-ID" -> "my_sps_entity_id", "MTDITID" -> "my_mtditid")

      When("GET /confirmation is called with an sps entity id in the session")
      IncomeTaxSubscriptionFrontend.confirmation(sessionMap)
      Then("SPS confirmation service is called with appropriate payload")
      verifyPost("/channel-preferences/confirm", Some("""{"entityId":"my_sps_entity_id","itsaId":"HMRC-MTD-IT~MTDITID~my_mtditid"}"""), Some(1))
    }

    "not confirm SPS preferences" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(Map(
        IncomeSource -> Json.toJson(testIncomeSourceIndivProperty),
        MtditId -> JsString(testSubscriptionId)
      ))
      And("The SPS feature switch is disabled")
      disable(SPSEnabled)
      When("GET /confirmation is called")
      IncomeTaxSubscriptionFrontend.confirmation()
      Then("SPS confirmation service is not called with approriate payload")
      verifyPost("/channel-preferences/confirm", None, Some(0))
    }

    "also not confirm SPS preferences" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubEnrolled()
      IncomeTaxSubscriptionConnectorStub.stubSubscriptionData(Map(
        IncomeSource -> Json.toJson(testIncomeSourceIndivProperty),
        MtditId -> JsString(testSubscriptionId)
      ))
      And("The SPS feature switch is enabled, but no entity id is present")
      enable(SPSEnabled)
      val sessionMap = Map("MTDITID" -> "my_mtditid")
      When("GET /confirmation is called")
      IncomeTaxSubscriptionFrontend.confirmation(sessionMap)
      Then("SPS confirmation service is not called")
      verifyPost("/channel-preferences/confirm", None, Some(0))
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
