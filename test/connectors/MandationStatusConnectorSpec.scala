/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MockHttp
import connectors.httpparser.PostMandationStatusParser.PostMandationStatusResponse
import models.ErrorModel
import models.status.MandationStatus.Voluntary
import models.status.{MandationStatusModel, MandationStatusRequest}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.Future

class MandationStatusConnectorSpec extends MockHttp with Matchers {
  implicit val request: Request[_] = FakeRequest()
  val connector = new MandationStatusConnector(appConfig, mockHttp)

  val headers = Seq()

  "getMandationStatus" should {
    "retrieve the user mandation status" when {
      "the status-determination-service returns a successful response" in {
        when(mockHttp.POST[MandationStatusRequest, PostMandationStatusResponse](
          ArgumentMatchers.eq(appConfig.mandationStatusUrl),
          ArgumentMatchers.eq(MandationStatusRequest("test-nino", "test-utr")),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(MandationStatusModel(currentYearStatus = Voluntary, nextYearStatus = Voluntary))))

        await(
          connector.getMandationStatus("test-nino", "test-utr")
        ) mustBe Right(MandationStatusModel(currentYearStatus = Voluntary, nextYearStatus = Voluntary))
      }
    }

    "return an error" when {
      "the status-determination-service returns a failed response" in {
        when(mockHttp.POST[MandationStatusRequest, PostMandationStatusResponse](
          ArgumentMatchers.eq(appConfig.mandationStatusUrl),
          ArgumentMatchers.eq(MandationStatusRequest("test-nino", "test-utr")),
          ArgumentMatchers.any()
        )(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, """{"code":"code","reason":"reason"}"""))))

        await(
          connector.getMandationStatus("test-nino", "test-utr")
        ) mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, """{"code":"code","reason":"reason"}"""))
      }
    }
  }
}
