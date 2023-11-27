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

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.{basGatewaySignIn, IndividualURI}
import helpers.servicemocks.AuthStub
import play.api.http.Status._

class SPSCallbackControllerISpec extends ComponentSpecBase {
  s"GET ${controllers.individual.sps.routes.SPSCallbackController.callback(None).url}" when {
    "the user is not authorised" should {
      "redirect the user to login" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.spsCallback(hasEntityId = true)

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/sps-callback"))
        )
      }
    }

    "the user is authorised" should {
      "redirect the user to the task list page" when {
        "entity id is present" in {
          AuthStub.stubAuthSuccess()

          val res = IncomeTaxSubscriptionFrontend.spsCallback(hasEntityId = true)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.whatYouNeedToDoURI)
          )
        }
      }

      "return an internal server error" when {
        "entity id is not present" in {
          AuthStub.stubAuthSuccess()

          val res = IncomeTaxSubscriptionFrontend.spsCallback(hasEntityId = false)
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }
}
