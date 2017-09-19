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

package services.mocks

import connectors.mocks.MockAddressLookupConnector
import connectors.models.address.{AddressLookupFailureResponse, AddressLookupRequest}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.BAD_REQUEST
import services.AddressLookupService
import utils.MockTrait
import utils.TestConstants.testException

import scala.concurrent.Future


trait MockAddressLookupService extends MockTrait {

  val mockAddressLookupService = mock[AddressLookupService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressLookupService)
  }

  private def mockInit(request: AddressLookupRequest)(response: Future[Either[AddressLookupFailureResponse, String]]) =
    when(mockAddressLookupService.init(ArgumentMatchers.eq(request))(ArgumentMatchers.any())).thenReturn(response)

  def mockInitSuccess(request: AddressLookupRequest)(testRedirectionUrl: String) = mockInit(request)(Right(testRedirectionUrl))

  def mockInitFailure(request: AddressLookupRequest) = mockInit(request)(Future.successful(Left(AddressLookupFailureResponse(BAD_REQUEST))))

  def mockInitException(request: AddressLookupRequest) = mockInit(request)(Future.failed(testException))

}


trait TestAddressLookupService extends MockAddressLookupConnector {

  object TestAddressLookupService extends AddressLookupService(
    mockAddressLookupConnector
  )

}