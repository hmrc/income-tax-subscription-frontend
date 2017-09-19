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

package services

import connectors.httpparsers.AddressLookupResponseHttpParser.InitAddressLookupResponseResponse
import connectors.models.address.{AddressLookupFailureResponse, AddressLookupRequest}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import services.mocks.TestAddressLookupService

import scala.concurrent.Future


class AddressLookupServiceSpec extends TestAddressLookupService with ScalaFutures {

  val testContinueUrl = "testContinueUrl"
  val testRedirectionUrl = "testRedirectionUrl"
  val testRequest = AddressLookupRequest(testContinueUrl)

  "AddressLookupService.init" should {
    def call: Future[InitAddressLookupResponseResponse] = TestAddressLookupService.init(testRequest)

    "return a success if the AddressLookupConnector returns a success" in {
      mockInitSuccess(testRequest)(testRedirectionUrl)

      val result = call

      whenReady(result)(_ mustBe Right(testRedirectionUrl))
    }

    "return a failure if the AddressLookupConnector returns a failure" in {
      mockInitFailure(testRequest)

      val result = call

      whenReady(result)(_ mustBe Left(AddressLookupFailureResponse(BAD_REQUEST)))
    }

  }

}
