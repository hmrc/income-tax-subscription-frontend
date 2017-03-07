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

package services

import auth.{IncomeTaxSAUser, authenticatedFakeRequest}
import org.scalatest.Matchers._
import play.api.test.Helpers._
import services.mocks.MockThrottlingService
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Authority, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.UnitTestTrait

class ThrottlingServiceSpec extends UnitTestTrait
  with MockThrottlingService {


  object TestUser {

    import auth._
    import auth.ggUser._

    lazy val noNinoAccounts = domain.Accounts(org = Some(domain.OrgAccount(link = "/paye/abc", org = Org(""))))

    val noNinoAuth: Authority =
      Authority(mockAuthorisedUserIdCL200,
        noNinoAccounts,
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L200,
        None,
        None,
        None,
        ""
      )
    val noNinoUserContext = AuthContext(
      authority = noNinoAuth,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )
  }

  "ThrottlingService" should {

    implicit lazy val request = authenticatedFakeRequest()
    implicit lazy val hc = HeaderCarrier()

    "if there's a nino present for the user, call the throttling connector.check access" in {
      implicit val user = IncomeTaxSAUser(auth.ggUser.userCL200Context)
      setupMockCheckAccess(auth.nino)(OK)
      await(TestThrottlingService.checkAccess)
      verifyMockCheckAccess(1)
    }

    "if there's a nino present for the user, do not call the throttling connector.check access" in {
      implicit val user = IncomeTaxSAUser(TestUser.noNinoUserContext)

      setupMockCheckAccess(auth.nino)(OK)

      val thrown = intercept[Exception] {
        await(TestThrottlingService.checkAccess)
      }

      thrown.isInstanceOf[InternalServerException] shouldBe true

      verifyMockCheckAccess(0)
    }

  }

}
