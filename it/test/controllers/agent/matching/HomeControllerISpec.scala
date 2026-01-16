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

package controllers.agent.matching

import auth.agent.{AgentSignUp, AgentUserMatching}
import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.IntegrationTestConstants.{testNino, testUtr}
import helpers.agent.servicemocks.AuthStub
import helpers.agent.{ComponentSpecBase, SessionCookieCrumbler}
import models.EligibilityStatus
import play.api.http.Status._
import play.api.libs.json.{JsBoolean, JsString, Json}
import utilities.SubscriptionDataKeys

class HomeControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  s"GET ${routes.HomeController.home.url}" should {
    "return a redirect to the index page" in {
      val res = IncomeTaxSubscriptionFrontend.startPage()

      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.matching.routes.HomeController.index.url)
      )
    }
  }

  s"GET ${routes.HomeController.index.url}" when {
    "auth is successful" when {
      "the agent is in a user matching state" should {
        "redirect to the enter client details page" in {
          AuthStub.stubAuthSuccess()

          val res = IncomeTaxSubscriptionFrontend.indexPage(Some(AgentUserMatching))

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.UsingSoftwareController.show().url)
          )
        }
      }
      "the agent is in no state" should {
        "redirect to the add another client route" in {
          AuthStub.stubAuthSuccess()

          val res = IncomeTaxSubscriptionFrontend.indexPage(None)

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.agent.routes.UsingSoftwareController.show().url)
          )
        }
      }
    }
  }
}
