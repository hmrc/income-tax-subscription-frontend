/*
 * Copyright 2020 HM Revenue & Customs
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
import models.status.{MandationStatus, MandationStatusModel, MandationStatusRequest}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}

object MandationStatusStub extends WireMockMethods {
  def stubGetMandationStatus(expectedBody: JsValue)(status: Int, body: JsValue): StubMapping = {
    when(
      method = POST,
      uri = "/income-tax-subscription/itsa-status",
      body = expectedBody
    ).thenReturn(status, body)

  }

  def stubGetMandationStatus(nino: String, utr: String)(currentYear: MandationStatus, nextYear: MandationStatus): StubMapping = {
    stubGetMandationStatus(
      Json.toJson(MandationStatusRequest(nino, utr))
    )(OK, Json.toJson(MandationStatusModel(currentYearStatus = currentYear, nextYearStatus = nextYear)))
  }

  def stubGetMandationStatusInvalidResponse(expectedBody: JsValue)(status: Int, body: String): StubMapping = {
    when(
      method = POST,
      uri = "/income-tax-subscription/itsa-status",
      body = expectedBody
    ).thenReturn(status, body)
  }

  def stubGetMandationStatusInvalidResponse(nino: String, utr: String): StubMapping = {
    stubGetMandationStatusInvalidResponse(
      Json.toJson(MandationStatusRequest(nino, utr))
    )(INTERNAL_SERVER_ERROR, "")
  }
}
