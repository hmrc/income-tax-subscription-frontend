/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status._
import play.api.libs.json.Json

object EligibilityStub extends WireMockMethods {

  def stubEligibilityResponse(sautr: String)(response: Boolean): Unit =
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/utr/$sautr"
    ).thenReturn(
      status = OK,
      body = Json.obj("eligibleCurrentYear" -> response, "eligibleNextYear" -> false)
    )

  def stubEligibilityResponseBoth(sautr: String)(currentYearResponse: Boolean, nextYearResponse: Boolean, exemptionReason: Option[String]): Unit =
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/utr/$sautr"
    ).thenReturn(
      status = OK,
      body = Json.obj(
        "eligibleCurrentYear" -> currentYearResponse,
        "eligibleNextYear" -> nextYearResponse,
        "exemptionReason" -> exemptionReason
      )
    )

  def stubEligibilityResponseInvalid(sautr: String): Unit = {
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/utr/$sautr"
    ).thenReturn(
      status = OK,
      body = Json.obj()
    )
  }

  def stubEligibilityResponseError(sautr: String): Unit = {
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/utr/$sautr"
    ).thenReturn(
      status = INTERNAL_SERVER_ERROR
    )
  }

  def stubEligibilityResponseBoth(nino: String, utr: String)(currentYearResponse: Boolean, nextYearResponse: Boolean): Unit = {
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/nino/$nino/utr/$utr"
    ).thenReturn(
      status = OK,
      body = Json.obj(
        "eligibleCurrentYear" -> currentYearResponse,
        "eligibleNextYear" -> nextYearResponse
      )
    )
  }

  def stubEligibilityResponseInvalid(nino: String, sautr: String): Unit = {
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/nino/$nino/utr/$sautr"
    ).thenReturn(
      status = OK,
      body = Json.obj()
    )
  }

  def stubEligibilityResponseError(nino: String, sautr: String): Unit = {
    when(
      method = GET,
      uri = s"/income-tax-subscription-eligibility/eligibility/nino/$nino/utr/$sautr"
    ).thenReturn(
      status = INTERNAL_SERVER_ERROR
    )
  }

}
