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
import config.featureswitch.FeatureSwitch.UseIdempotency
import connectors.stubs.SignUpAPIStub
import helpers.ComponentSpecBase
import models.common.subscription.{SignUpFailureResponse, SignUpRequestModel, SignUpSuccessful}
import models.{Current, Next}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UUIDProvider

import scala.collection.mutable

class SignUpConnectorISpec extends ComponentSpecBase with BeforeAndAfterEach {

  private lazy val testUUIDProvider = new TestUUIDProvider

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(bind[UUIDProvider].to(testUUIDProvider))
    .build()

  override protected def afterEach(): Unit = {
    testUUIDProvider.setKeys(Seq("test-uuid"))
    disable(UseIdempotency)
    super.afterEach()
  }

  "signUp when UseIdempotency feature switch is disabled" should {
    "return a sign up success response for the current year without idempotency key" in {
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
        status = OK,
        json = Json.obj("mtdbsa" -> mtdbsa)
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "return a sign up success response for the next year without idempotency key" in {
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Next))(
        status = OK,
        json = Json.obj("mtdbsa" -> mtdbsa)
      )

      val result = connector.signUp(nino, utr, Next)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "return InvalidJson when an OK response body cannot be parsed" in {
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
        status = OK
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.InvalidJson)
    }

    "return UnprocessableSignUp when a 422 response has a code and reason" in {
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
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
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
        status = UNPROCESSABLE_ENTITY
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.InvalidJson)
    }

    "return UnexpectedStatus for an unhandled upstream status" in {
      disable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current))(
        status = INTERNAL_SERVER_ERROR
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnexpectedStatus(INTERNAL_SERVER_ERROR))
    }
  }

  "signUp when UseIdempotency feature switch is enabled" should {
    "return a sign up success response with idempotency key in the request" in {
      enable(UseIdempotency)

      SignUpAPIStub.stubSignUp(SignUpRequestModel(nino, utr, Current, idempotencyKey = Some("test-uuid")))(
        status = OK,
        json = successJson
      )

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Right(SignUpSuccessful(mtdbsa))
    }

    "retry with the same idempotency key when SERVICE_UNAVAILABLE (503) is returned" in {
      enable(UseIdempotency)
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
      enable(UseIdempotency)
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
      enable(UseIdempotency)
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
      enable(UseIdempotency)
      testUUIDProvider.setKeys(Seq("key-1", "key-2"))

      SignUpAPIStub.stubIdempotencyRetryNewKeyScenario(
        scenarioName = "retry-different-key",
        firstAttemptKey = "key-1",
        secondAttemptKey = "key-2",
        retryableCode = "003",
        successBody = successJson
      )

      val result = await(connector.signUp(nino, utr, Current))

      result mustBe Right(SignUpSuccessful(mtdbsa))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-1")
      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 1, idempotencyKey = "key-2")
    }

    "return the final error when retries are exhausted for a retryable status" in {
      enable(UseIdempotency)
      testUUIDProvider.setKeys(Seq("key-1"))

      SignUpAPIStub.stubIdempotencyAlwaysFailWithSameKey(SERVICE_UNAVAILABLE, "key-1")

      val result = connector.signUp(nino, utr, Current)

      await(result) mustBe Left(SignUpFailureResponse.UnexpectedStatus(SERVICE_UNAVAILABLE))

      SignUpAPIStub.verifyIdempotencyKeyRequestCount(expectedCount = 4, idempotencyKey = "key-1")
    }

    "not retry when 422 has a non-retryable code" in {
      enable(UseIdempotency)
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
    }
  }

  private class TestUUIDProvider extends UUIDProvider {
    private val remaining: mutable.Queue[String] = mutable.Queue("test-uuid")

    def setKeys(keys: Seq[String]): Unit = {
      remaining.clear()
      remaining.addAll(keys)
    }

    override def getUUID: String = if (remaining.nonEmpty) remaining.dequeue() else "test-uuid"
  }

  private lazy val successJson: play.api.libs.json.JsObject = Json.obj("mtdbsa" -> mtdbsa)

  private lazy val signUpUri: String = "/income-tax-subscription/mis/sign-up"

  lazy val connector: SignUpConnector = app.injector.instanceOf[SignUpConnector]
  lazy val nino: String = "test-nino"
  lazy val utr: String = "test-utr"
  lazy val mtdbsa: String = "test-mtdbsa"

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

}
