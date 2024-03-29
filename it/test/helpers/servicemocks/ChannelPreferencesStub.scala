/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.http.Status.OK
import play.api.libs.json.Json


object ChannelPreferencesStub extends WireMockMethods {

  val channelPreferencesUrl = "/channel-preferences/confirm"
  val channelPreferencesAgentUrl = "/channel-preferences/enrolment"

  def stubChannelPreferenceConfirm(): Unit = {
    when (
      method = POST,
      uri = channelPreferencesUrl
    ).thenReturn (
      status = OK,
      body = Json.obj()
    )
  }

  def stubAgentChannelPreferencesConfirm(): Unit = {
    when(
      method = POST,
      uri = channelPreferencesAgentUrl
    ).thenReturn (
      status = OK,
      body = Json.obj()
    )
  }

}
