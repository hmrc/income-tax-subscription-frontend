/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.test.FakeRequest

class AuthorisedForIncomeTaxSASpec extends UnitSpec with WithFakeApplication {

  "Calling authenticated async action with no logged in session" should {
    "result in a redirect to login" in {
      val result = AuthTestController.authorisedAsyncAction(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 500" should {
    "result in an OK status" in {
      val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL500))
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 200 and a PAYE account" should {
    "result in an OK status" in {
      val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200))
      status(result) shouldBe Status.OK
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 200 and NO PAYE account" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL200NoAccounts))

    "result in a status SEE_OTHER (303) redirect" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the IV uplift journey" in {
      redirectLocation(result) shouldBe Some(ivRegisterURI.toString)
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 100" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL100))

    "result in a status SEE_OTHER (303) redirect" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the IV uplift journey" in {
      redirectLocation(result) shouldBe Some(ivUpliftURI.toString)
    }
  }

  "Calling authenticated async action with a logged in user with Confidence Level 50" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockUpliftUserIdCL50))

    "result in a status SEE_OTHER (303) redirect" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the IV uplift journey" in {
      redirectLocation(result) shouldBe Some(ivUpliftURI.toString)
    }
  }

  "Calling authenticated action with a logged in user with weak credentials" should {

    lazy val result = AuthTestController.authorisedAsyncAction(authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockWeakUserId))

    "result in a redirect status" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to 2FA service" in {
      redirectLocation(result) shouldBe Some(twoFactorURI.toString)
    }
  }

  "Calling authenticated action with a timed out user session" should {

    lazy val result = AuthTestController.authorisedAsyncAction(timeoutFakeRequest())

    "result in a redirect status" in {
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to the Session Timeout Page" in {
      redirectLocation(result) shouldBe Some(controllers.routes.SessionTimeoutController.timeout().url)
    }
  }
}
