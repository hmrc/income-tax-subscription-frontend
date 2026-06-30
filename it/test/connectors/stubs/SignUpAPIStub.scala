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

package connectors.stubs

import com.github.tomakehurst.wiremock.client.WireMock as WireMockClient
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.servicemocks.WireMockMethods
import models.common.subscription.SignUpRequestModel
import play.api.http.ContentTypes
import play.api.http.Status.UNPROCESSABLE_ENTITY
import play.api.libs.json.{JsValue, Json}

object SignUpAPIStub extends WireMockMethods {

  private def signUpUri: String = s"/income-tax-subscription/mis/sign-up"

  def stubSignUp(signUpModel: SignUpRequestModel)(status: Int, json: JsValue = Json.obj()): StubMapping = {
    when(
      method = POST,
      uri = signUpUri,
      body = signUpModel
    ).thenReturn(
      status = status,
      body = json
    )
  }

  def stubIdempotencyRetrySameKeyScenario(scenarioName: String,
                                          firstAttemptStatus: Int,
                                          idempotencyKey: String,
                                          successBody: JsValue): Unit = {
    val secondState = s"$scenarioName-second-attempt"

    stubFor(
      post(urlEqualTo(signUpUri))
        .inScenario(scenarioName)
        .whenScenarioStateIs(STARTED)
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(idempotencyKey)))
        .willSetStateTo(secondState)
        .willReturn(
          aResponse()
            .withStatus(firstAttemptStatus)
            .withHeader("Content-Type", ContentTypes.JSON)
            .withBody("{}")
        )
    )

    stubFor(
      post(urlEqualTo(signUpUri))
        .inScenario(scenarioName)
        .whenScenarioStateIs(secondState)
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(idempotencyKey)))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", ContentTypes.JSON)
            .withBody(successBody.toString())
        )
    )
  }

  def stubIdempotencyRetryNewKeyScenario(scenarioName: String,
                                         firstAttemptKey: String,
                                         secondAttemptKey: String,
                                         retryableCode: String,
                                         successBody: JsValue): Unit = {
    val secondState = s"$scenarioName-second-attempt"

    stubFor(
      post(urlEqualTo(signUpUri))
        .inScenario(scenarioName)
        .whenScenarioStateIs(STARTED)
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(firstAttemptKey)))
        .willSetStateTo(secondState)
        .willReturn(
          aResponse()
            .withStatus(UNPROCESSABLE_ENTITY)
            .withHeader("Content-Type", ContentTypes.JSON)
            .withBody(Json.obj("code" -> retryableCode, "reason" -> "retry").toString())
        )
    )

    stubFor(
      post(urlEqualTo(signUpUri))
        .inScenario(scenarioName)
        .whenScenarioStateIs(secondState)
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(secondAttemptKey)))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", ContentTypes.JSON)
            .withBody(successBody.toString())
        )
    )
  }

  def stubIdempotencyAlwaysFailWithSameKey(status: Int, idempotencyKey: String): Unit = {
    stubFor(
      post(urlEqualTo(signUpUri))
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(idempotencyKey)))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", ContentTypes.JSON)
            .withBody("{}")
        )
    )
  }

  def verifyIdempotencyKeyRequestCount(expectedCount: Int, idempotencyKey: String): Unit = {
    WireMockClient.verify(
      expectedCount,
      postRequestedFor(urlEqualTo(signUpUri))
        .withRequestBody(matchingJsonPath("$.idempotencyKey", equalTo(idempotencyKey)))
    )
  }

}
