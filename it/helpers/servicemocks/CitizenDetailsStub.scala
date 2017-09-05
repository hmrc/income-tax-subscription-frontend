/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.http.Status._

object CitizenDetailsStub extends WireMockMethods {

  def successResponse(nino: String, utr: Option[String]): String =
    s"""
       |{
       |  "name": {
       |    "current": {
       |      "firstName": "Test",
       |      "lastName": "User"
       |    },
       |    "previous": []
       |  },
       |  "ids": {
       |    "nino": "$nino"${utr.fold("")(x => s""","sautr"="$x"""")}
       |  },
       |  "dateOfBirth": "11121971"
       |}
    """.stripMargin

  private def stubCitizenDetails(nino: String)(status: Int, body: String): Unit =
    when(method = GET, uri = s"/citizen-details/nino/$nino")
      .thenReturn(status = status, body = body)

  def stubCIDUserWithNinoAndUtr(nino: String, utr: String): Unit =
    stubCitizenDetails(nino)(OK, successResponse(nino, Some(utr)))

}
