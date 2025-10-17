/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.eligibility.individual

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import play.api.http.Status.{OK, SEE_OTHER}

class SigningUpControllerISpec extends ComponentSpecBase {

  private val path = s"${controllers.eligibility.individual.routes.SigningUpController.show.url}"

  s"GET $path" should {
    "return OK" in {
      val res = IncomeTaxSubscriptionFrontend.individualSigningUp()
      res must have(
        httpStatus(OK)
      )
    }
  }

  s"POST $path" should {
    "redirect to the start of the agent sign up" in {
      val submitResult = IncomeTaxSubscriptionFrontend.individualSigningUpPost()
      submitResult must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.youCanSignUpNow)
      )
    }
  }
}
