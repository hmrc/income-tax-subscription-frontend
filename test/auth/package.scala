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

import java.net.{URI, URLEncoder}
import config.AppConfig
import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, domain}
import java.util.UUID
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProviderIds}
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.time.DateTimeUtils

package object auth {

  val mockConfig: AppConfig = MockConfig
  val mockAuthConnector: AuthConnector = MockAuthConnector
  val nino = "AB124512C"
  val authorisedUserAccounts = domain.Accounts(paye = Some(domain.PayeAccount(link = "/paye/abc", nino = Nino(nino))))
  val noAuthorisedUserAccounts = domain.Accounts(paye = None)

  val ivUpliftURI: URI =
    new URI(s"${mockConfig.ivUpliftUrl}?origin=SABR&" +
      s"completionURL=${URLEncoder.encode("/income-tax-subscription-frontend/hello-world", "UTF-8")}&" +
      s"failureURL=${URLEncoder.encode(mockConfig.notAuthorisedRedirectUrl, "UTF-8")}" +
      s"&confidenceLevel=200")

  val twoFactorURI: URI =
    new URI(s"${mockConfig.twoFactorUrl}?" +
      s"continue=${URLEncoder.encode("/income-tax-subscription-frontend/hello-world", "UTF-8")}&" +
      s"failure=${URLEncoder.encode(mockConfig.notAuthorisedRedirectUrl, "UTF-8")}")

  object ggSession {
    val userId = "/auth/oid/1234567890"
    val oid = "1234567890"
    val governmentGatewayToken = "token"
    val name = "Dave Agent"
  }

  val UserIdBuilder: String => String = id => "/auth/oid/" + id

  val mockAuthorisedUserIdCL500 = UserIdBuilder("mockAuthorisedUserIdCL500")
  val mockAuthorisedUserIdCL200 = UserIdBuilder("mockAuthorisedUserIdCL200")
  val mockUpliftUserIdCL200NoAccounts = UserIdBuilder("mockUpliftUserIdCL200NoAccounts")
  val mockUpliftUserIdCL100 = UserIdBuilder("mockUpliftUserIdCL100")
  val mockUpliftUserIdCL50 = UserIdBuilder("mockUpliftUserIdCL50")
  val mockWeakUserId = UserIdBuilder("mockWeakUserId")
  val mockTimeout = UserIdBuilder("mockTimeout")

  object ggUser {

    // scalastyle:off
    val loggedInAt = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
    val previouslyLoggedInAt = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))
    val timedOutCreds = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
    // scalastyle:on

    val userCL500: Authority =
      Authority(mockAuthorisedUserIdCL500,
        authorisedUserAccounts,
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L500,
        None,
        None,
        None,
        ""
      )
    val userCL500Context = AuthContext(
      authority = userCL500,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )


    val userCL200: Authority =
      Authority(mockAuthorisedUserIdCL200,
        authorisedUserAccounts,
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L200,
        None,
        None,
        None,
        ""
      )
    val userCL200Context = AuthContext(
      authority = userCL200,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )


    val userCL200NoAccounts: Authority =
      Authority(mockUpliftUserIdCL200NoAccounts,
        Accounts(),
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L200,
        None,
        None,
        None,
        ""
      )
    val userCL200NoAccountsContext = AuthContext(
      authority = userCL200NoAccounts,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )


    val userCL100: Authority =
      Authority(mockUpliftUserIdCL100,
        authorisedUserAccounts,
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L100,
        None,
        None,
        None,
        ""
      )
    val userCL100Context = AuthContext(
      authority = userCL100,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )


    val userCL50: Authority =
      Authority(mockUpliftUserIdCL50,
        authorisedUserAccounts,
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Strong,
        ConfidenceLevel.L50,
        None,
        None,
        None,
        ""
      )
    val userCL50Context = AuthContext(
      authority = userCL50,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )


    val weakStrengthUser: Authority =
      Authority(mockWeakUserId,
        Accounts(),
        loggedInAt,
        previouslyLoggedInAt,
        CredentialStrength.Weak,
        ConfidenceLevel.L200,
        None,
        None,
        None,
        ""
      )
    val weakStrengthUserContext = AuthContext(
      authority = weakStrengthUser,
      governmentGatewayToken = Some(ggSession.governmentGatewayToken),
      nameFromSession = Some(ggSession.name)
    )
  }

  lazy val fakeRequest = FakeRequest()

  def authenticatedFakeRequest(provider: String = AuthenticationProviderIds.GovernmentGatewayId,
                               userId: String = mockAuthorisedUserIdCL200): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.userId-> userId,
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString,
      SessionKeys.token -> "ANYOLDTOKEN",
      SessionKeys.authProvider -> provider
  )

  def timeoutFakeRequest(provider: String = AuthenticationProviderIds.GovernmentGatewayId,
                         userId: String = mockAuthorisedUserIdCL200): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.userId -> userId,
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.minusDays(1).getMillis.toString,
      SessionKeys.token -> "ANYOLDTOKEN",
      SessionKeys.authProvider -> provider,
      SessionKeys.otacToken -> "ANYOLDTOKEN"
    )
}
