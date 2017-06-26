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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connectors.models.Enrolment
import helpers.IntegrationTestConstants._
import models.auth.UserIds
import org.joda.time.{DateTime, DateTimeZone}
import play.api.http.Status._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.connectors.domain
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Authority, ConfidenceLevel, CredentialStrength, PayeAccount}

object AuthStub extends WireMockMethods {
  private val authIDs = "/uri/to/ids"
  private val authority = "/auth/authority"

  private val gatewayID = "12345"
  private val internalID = "internal"
  private val externalID = "external"
  private val loggedInAt: Some[DateTime] = Some(new DateTime(2015, 11, 22, 11, 33, 15, 234, DateTimeZone.UTC))
  private val previouslyLoggedInAt: Some[DateTime] = Some(new DateTime(2014, 8, 3, 9, 25, 44, 342, DateTimeZone.UTC))
  private val userIDs = UserIds(internalId = internalID, externalId = externalID)

  def stubAuthSuccess(): StubMapping = {
    stubAuthoritySuccess()
    stubAuthorityUserIDsSuccess()
    stubEnrolments()
  }

  private def stubAuthoritySuccess(): StubMapping =
    when(method = GET, uri = authority)
      .thenReturn(status = OK, body = successfulAuthResponse)

  private def successfulAuthResponse: Authority = Authority(
    uri = userId,
    accounts = domain.Accounts(paye = Some(PayeAccount("", Nino(testNino)))),
    loggedInAt = loggedInAt,
    previouslyLoggedInAt = previouslyLoggedInAt,
    credentialStrength = CredentialStrength.Strong,
    confidenceLevel = ConfidenceLevel.L500,
    userDetailsLink = None,
    enrolments = None,
    ids = None,
    legacyOid = ""
  )

  private def stubAuthorityUserIDsSuccess(): StubMapping =
    when(method = GET, uri = authIDs)
      .thenReturn(status = OK, body = userIDs)

  private def stubEnrolments(): StubMapping =
    when(method = GET, uri = s"$userId/enrolments")
      .thenReturn(status = OK, body = Seq.empty[Enrolment])
}
