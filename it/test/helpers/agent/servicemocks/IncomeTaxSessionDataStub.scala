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

package helpers.agent.servicemocks

import play.api.libs.json.Json

object IncomeTaxSessionDataStub extends WireMockMethods {

  def stubSetupViewAndChangeData(mtditid: String, nino: String, utr: String)(status: Int): Unit = {
    when(
      method = POST,
      uri = "/income-tax-session-data",
      body = Json.obj(
        "mtditid" -> mtditid,
        "nino" -> nino,
        "utr" -> utr
      )
    ).thenReturn(status = status)
  }

}
