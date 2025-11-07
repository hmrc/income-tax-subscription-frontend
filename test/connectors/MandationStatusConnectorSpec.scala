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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import config.AppConfig
import models.ErrorModel
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import scala.concurrent.ExecutionContext

class MandationStatusConnectorSpec extends AnyWordSpec with Matchers with WireMockSupport with HttpClientV2Support {

  private lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.income-tax-subscription.host" -> wireMockHost,
      "microservice.services.income-tax-subscription.port" -> wireMockPort,
      "microservice.services.income-tax-subscription.protocol" -> "http"
    )
    .build()

  override def beforeEach(): Unit = super.beforeEach()

  private val appConfig = app.injector.instanceOf[AppConfig]

  implicit val request: Request[_] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val connector = new MandationStatusConnector(appConfig, httpClientV2)

  val headers = Seq()

  "getMandationStatus" should {
    "retrieve the user mandation status" when {
      "the status-determination-service returns a successful response" in {
        val responseJson = Json.toJson(MandationStatusModel(currentYearStatus = Voluntary, nextYearStatus = Voluntary))

        stubFor(
          post(urlEqualTo("/income-tax-subscription/itsa-status"))
            .withRequestBody(equalToJson("""{"nino":"test-nino","utr":"test-utr"}""", true, true))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withHeader("Content-Type", "application/json")
                .withBody(responseJson.toString())
            )
        )

        await(connector.getMandationStatus("test-nino", "test-utr")
        ) mustBe Right(MandationStatusModel(Voluntary, Voluntary))
      }
    }

    "return an error" when {
      "the status-determination-service returns a failed response" in {
        stubFor(
          post(urlEqualTo("/income-tax-subscription/itsa-status"))
            .withRequestBody(equalToJson("""{"nino":"test-nino","utr":"test-utr"}"""))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"code":"code","reason":"reason"}""")
            )
        )

        await(
          connector.getMandationStatus("test-nino", "test-utr")
        ) mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, """{"code":"code","reason":"reason"}"""))
      }
    }
  }
}