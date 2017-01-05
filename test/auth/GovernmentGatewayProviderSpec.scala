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

package auth

import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.test.Helpers._

class GovernmentGatewayProviderSpec extends UnitSpec with WithFakeApplication {

  lazy val ggw = new GovernmentGatewayProvider(MockConfig.ggSignInContinueUrl, loginUrl = MockConfig.ggSignInUrl)

  "Government Gateway Provider" should {
    "have a login url set from its second constructor parameter" in {
      ggw.loginURL shouldEqual MockConfig.ggSignInUrl
    }
  }

  "Government Gateway Provider" should {
    "have a continueURL constructed from its first constructor parameter, encoded" in {
      ggw.continueURL shouldEqual MockConfig.ggSignInContinueUrl
    }
  }

  "Government Gateway Provider" should {
    "handle a session timeout with a redirect" in {
      status(ggw.handleSessionTimeout(FakeRequest())) shouldBe Status.SEE_OTHER
    }

    "have a redirect location to the session timeout controller route" in {
      redirectLocation(ggw.handleSessionTimeout(FakeRequest())) shouldBe Some(controllers.routes.SessionTimeoutController.timeout().url)
    }
  }
}
