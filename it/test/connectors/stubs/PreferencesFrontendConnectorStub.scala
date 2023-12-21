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

import helpers.servicemocks.WireMockMethods
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json

object PreferencesFrontendConnectorStub extends WireMockMethods {

  def activateUri = s"/paperless/activate\\?returnUrl=(.*)&returnLinkText=(.*)"

  def stubGetOptedInStatus(response: Option[Boolean]): Unit = {
    response match {
      case Some(value) =>
        when(
          method = PUT,
          uri = activateUri,
          body = Json.obj()
        ).thenReturn(OK, Json.obj("optedIn" -> value))
      case None =>
        when(
          method = PUT,
          uri = activateUri,
          body = Json.obj()
        ).thenReturn(NOT_FOUND)
    }
  }

}
