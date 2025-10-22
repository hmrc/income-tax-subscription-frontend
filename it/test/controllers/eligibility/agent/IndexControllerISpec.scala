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

package controllers.eligibility.agent

import helpers.{ComponentSpecBase, CustomMatchers}
import helpers.IntegrationTestConstants.AgentURI
import play.api.test.Helpers._

class IndexControllerISpec extends ComponentSpecBase with CustomMatchers {

  "GET /client" when {
    s"return $SEE_OTHER" in {
      val res = IncomeTaxSubscriptionFrontend.agentIndex()
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(AgentURI.signingUp)
      )
    }
  }

}
