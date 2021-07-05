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

package controllers.individual.sps

import config.featureswitch.FeatureSwitch.SPSEnabled
import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import play.api.http.Status.{SEE_OTHER, _}

class SPSHandoffControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(SPSEnabled)
  }

  s"GET ${controllers.individual.sps.routes.SPSHandoffController.redirectToSPS().url}" when {

    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.spsHandoff

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/sps-handoff"))
        )
      }
    }

    "the feature switch SPSEnabled set to true" in {
      Given("I setup the Wiremock stubs")
      enable(SPSEnabled)
      AuthStub.stubAuthSuccess()

      When("GET /sps-handoff is called")
      val res = IncomeTaxSubscriptionFrontend.spsHandoff()

      Then("Should return a SEE_OTHER and redirect to SPS")
      res should have(
        httpStatus(SEE_OTHER),
        redirectURI(spsHandoffURI)
      )
    }

    "the feature switch SPSEnabled set to false" in {
      Given("I setup the Wiremock stubs")
      AuthStub.stubAuthSuccess()

      When("GET /sps-handoff is called")
      val res = IncomeTaxSubscriptionFrontend.spsHandoff()

      Then("Should return a not found page to the user")
      res should have(
        httpStatus(NOT_FOUND),
        pageTitle("Page not found - 404")
      )
    }


  }

}