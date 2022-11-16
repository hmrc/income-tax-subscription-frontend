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

import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}

object CitizenDetailsStub extends WireMockMethods {

  def successResponse(nino: String, utr: Option[String], firstName: Option[String], lastName: Option[String]): JsObject =
    Json.obj(
      "ids" -> {
        Json.obj("nino" -> s"$nino")
          .++(utr.fold(Json.obj())(x => Json.obj("sautr" -> x)))
      },
      "name" -> Json.obj(
        "current" -> {
          firstName.fold(Json.obj())(x => Json.obj("firstName" -> x)) ++
            lastName.fold(Json.obj())(x => Json.obj("lastName" -> x))
        }
      )
    )

  private def stubCitizenDetails(nino: String)(status: Int, body: JsObject): Unit =
    when(method = GET, uri = s"/citizen-details/nino/$nino")
      .thenReturn(status = status, body = body)

  def stubCIDUserWithNinoAndUtrAndName(nino: String, utr: String, firstName: String, lastName: String): Unit =
    stubCitizenDetails(nino)(OK, successResponse(nino, Some(utr), Some(firstName), Some(lastName)))

  def stubCIDUserWithNinoAndUtrAndNoName(nino: String, utr: String): Unit =
    stubCitizenDetails(nino)(OK, successResponse(nino, Some(utr), None, None))

  def stubCIDUserWithNoUtr(nino: String): Unit =
    stubCitizenDetails(nino)(OK, successResponse(nino, None, None, None))

  def stubCIDNotFound(nino: String): Unit =
    stubCitizenDetails(nino)(NOT_FOUND, Json.obj())

}
