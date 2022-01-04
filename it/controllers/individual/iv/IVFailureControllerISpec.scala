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

package controllers.individual.iv

import config.featureswitch.FeatureSwitch.IdentityVerification
import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class IVFailureControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(IdentityVerification)
    super.beforeEach()
  }

  s"GET ${controllers.individual.iv.routes.IVFailureController.failure.url}" when {

    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.ivFailure()

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI("http://localhost:9553/bas-gateway/sign-in?continue_url=%2Freport-quarterly%2Fincome-and-expenses%2Fsign-up%2Fiv-failure&origin=income-tax-subscription-frontend")
        )
      }
    }

    "the identity verification feature switch is disabled" should {
      "return a not found page to the user" in {
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.ivFailure()

        res should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404")
        )
      }
    }

    "the identity verification feature switch is enabled" should {
      "redirect the user to the home page" in {
        enable(IdentityVerification)
        AuthStub.stubAuthSuccess()

        val res = IncomeTaxSubscriptionFrontend.ivFailure()

        res should have(
          httpStatus(OK),
          pageTitle(messages("titlePattern.serviceName.govUk", messages("iv-failure.heading")))
        )
      }
    }

  }

}
