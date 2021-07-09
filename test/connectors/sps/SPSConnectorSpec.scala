/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.sps

import config.AppConfig
import connectors.SPSConnector
import models.sps.SPSPayload
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpClient
import utilities.UnitTestTrait


class SPSConnectorSpec extends UnitTestTrait with MockitoSugar {

  "confirming sps preferences" should {
    "do a POST with appropriate url path" in {
      val appConfig = mock[AppConfig]
      val mockHttp = mock[HttpClient]
      when(appConfig.channelPreferencesUrl).thenReturn("microserviceurl")

      val connector = new SPSConnector(appConfig, mockHttp)
      val expected = Json.toJson(SPSPayload("my_entityid", "my_itsaid"))

      connector.postSpsConfirm("my_entityid","my_itsaid")

      verify(mockHttp).POST(
        ArgumentMatchers.eq("microserviceurl/channel-preferences/confirm"),
        ArgumentMatchers.eq(expected),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

  }
}
