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

import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import play.api.http.{HeaderNames, Status}


object AddressLookupStub extends WireMockMethods {

  val addressLookupURI: String = "/api/init"
  val addressFetchURI: String = s"/api/confirmed\\?id=$testId"

  def stubAddressSuccess(): Unit =
    when(method = POST, uri = addressLookupURI)
      .thenReturn(status = Status.ACCEPTED, headers = Map(HeaderNames.LOCATION -> testUrl))

  def stubAddressFailure(): Unit =
    when(method = POST, uri = addressLookupURI)
      .thenReturn(status = Status.BAD_REQUEST)

  def stubAddressFetchSuccess(): Unit =
    when(method = GET, uri = addressFetchURI)
      .thenReturn(status = Status.OK, body = testReturnedAddress)

  def stubAddressFetchFailure(): Unit =
    when(method = GET, uri = addressFetchURI)
      .thenReturn(status = Status.BAD_REQUEST)

}
