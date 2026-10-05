/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.stubs.SignUpAPIStub
import connectors.stubs.SignUpAPIStub.StubAttempt
import helpers.ComponentSpecBase
import models.common.subscription.{SignUpFailureResponse, SignUpRequestModel, SignUpSuccessful}
import models.{Current, Next}
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UUIDProvider

import scala.collection.mutable

class SignUpConnectorISpec extends ComponentSpecBase {

  private val testIdempotencyKey = "test-uuid"

  private class TestUUIDProvider extends UUIDProvider {
    private val keys: mutable.Queue[String] = mutable.Queue.empty

    def setKeys(values: Seq[String]): Unit = {
      keys.clear()
      keys.addAll(values)
    }

    override def getUUID: String = if (keys.nonEmpty) keys.dequeue() else testIdempotencyKey
  }

  private lazy val testUUIDProvider = new TestUUIDProvider

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(bind[UUIDProvider].to(testUUIDProvider))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    testUUIDProvider.setKeys(Seq(testIdempotencyKey))
  }

  "signUp responses" should {
    "return a sign up success response for the current year with an idempotency key" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = OK,
        json = Json.obj("mtdbsa" -> mtdbsa)
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "return a sign up success response for the next year with an idempotency key" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Next, idempotencyKey = Some(testIdempotencyKey)))(
        status = OK,
        json = Json.obj("mtdbsa" -> mtdbsa)
      )

      val result = connector.signUp(nino, utr, Next)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "return InvalidJson when an OK response body cannot be parsed" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = OK
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.InvalidJson)
    }

    "return UnprocessableSignUp when a 422 response has a code and reason" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = UNPROCESSABLE_ENTITY,
        json = Json.obj(
          "code" -> "500",
          "reason" -> "reason"
        )
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnprocessableSignUp("500", "reason"))
    }

    "return InvalidJson when a 422 response body cannot be parsed" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = UNPROCESSABLE_ENTITY
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.InvalidJson)
    }

    "return UnexpectedStatus for an unhandled upstream status" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = INTERNAL_SERVER_ERROR
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR))
    }
  }

  "signUp retries" should {
    "return a sign up success response with idempotency key in the request" in {

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some(testIdempotencyKey)))(
        status = OK,
        json = successJson
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "retry with the same idempotency key when SERVICE_UNAVAILABLE (503) is returned" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubIdempotencyRetrySameKeyScenario(
        scenarioName = "retry-same-key",
        firstAttemptStatus = SERVICE_UNAVAILABLE,
        idempotencyKey = "key-1",
        successBody = successJson
      )

      val result = await(connector.signUp(nino, utr, Current))

      result mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "key-1")
    }

    "retry with the same idempotency key when BAD_GATEWAY (502) is returned" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubIdempotencyRetrySameKeyScenario(
        scenarioName = "retry-502",
        firstAttemptStatus = BAD_GATEWAY,
        idempotencyKey = "key-1",
        successBody = successJson
      )

      val result = await(connector.signUp(nino, utr, Current))

      result mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "key-1")
    }

    "retry with the same idempotency key when GATEWAY_TIMEOUT (504) is returned" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubIdempotencyRetrySameKeyScenario(
        scenarioName = "retry-504",
        firstAttemptStatus = GATEWAY_TIMEOUT,
        idempotencyKey = "key-1",
        successBody = successJson
      )

      val result = await(connector.signUp(nino, utr, Current))

      result mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "key-1")
    }

    "retry with a new idempotency key when 422 code 003 is returned" in {
      testUUIDProvider.setKeys(Seq("key-1", "key-2"))

      SignUpAPIStub.stubUnprocessableThenSuccessScenario(
        scenarioName = "retry-different-key",
        firstAttemptKey = "key-1",
        secondAttemptKey = "key-2",
        code = "003",
        successBody = successJson
      )

      val result = await(connector.signUp(nino, utr, Current))

      result mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-1")
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-2")
    }

    "retry with the same idempotency key when 422 code 830 is returned" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubUnprocessableThenSuccessScenario(
        scenarioName = "retry-830",
        firstAttemptKey = "key-1",
        secondAttemptKey = "key-1",
        code = "830",
        successBody = successJson
      )

      await(connector.signUp(nino, utr, Current)) mustBe Right(SignUpSuccessful(mtdbsa))
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "key-1")
    }

    "keep the new idempotency key when 422 code 003 is followed by 422 code 830" in {
      testUUIDProvider.setKeys(Seq("key-1", "key-2", "key-3"))

      SignUpAPIStub.stubIdempotencyAttemptsScenario(
        scenarioName = "retry-003-then-830",
        attempts = Seq(
          StubAttempt("key-1", UNPROCESSABLE_ENTITY, Json.obj("code" -> "003", "reason" -> "retry")),
          StubAttempt("key-2", UNPROCESSABLE_ENTITY, Json.obj("code" -> "830", "reason" -> "retry")),
          StubAttempt("key-2", OK, successJson)
        )
      )

      await(connector.signUp(nino, utr, Current)) mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-1")
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 2, idempotencyKey = "key-2")
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 0, idempotencyKey = "key-3")
    }

    "return the final error when retries are exhausted for a retryable status" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubIdempotencyAlwaysFailWithSameKey(SERVICE_UNAVAILABLE, "key-1")

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnexpectedStatus(SERVICE_UNAVAILABLE))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 4, idempotencyKey = "key-1")
    }

    "return the final error when retries are exhausted for 422 code 830" in {
      testUUIDProvider.setKeys(Seq("key-1", "key-2"))

      SignUpAPIStub.stubIdempotencyAlwaysFailWithSameKey(
        status = UNPROCESSABLE_ENTITY,
        idempotencyKey = "key-1",
        body = Json.obj("code" -> "830", "reason" -> "retry")
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnprocessableSignUp("830", "retry"))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 4, idempotencyKey = "key-1")
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 0, idempotencyKey = "key-2")
    }

    "not retry when 422 has a non-retryable code" in {
      testUUIDProvider.setKeys(Seq("key-1"))

      stubFor(
        post(urlEqualTo(signUpUri))
          .withRequestBody(matchingJsonPath("$.idempotencyKey"))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"code":"500","reason":"not retryable"}""")
          )
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnprocessableSignUp("500", "not retryable"))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-1")
    }
  }

  private lazy val successJson: play.api.libs.json.JsObject = Json.obj("mtdbsa" -> mtdbsa)

  private lazy val signUpUri: String = "/income-tax-subscription/mis/sign-up"

  lazy val connector: SignUpConnector = app.injector.instanceOf[SignUpConnector]
  lazy val nino: String = "test-nino"
  lazy val utr: String = "test-utr"
  lazy val mtdbsa: String = "test-mtdbsa"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

}
