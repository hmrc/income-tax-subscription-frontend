/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors.individual.business.mocks

import connectors.individual.business.AddressLookupConnector
import core.utils.MockTrait
import core.utils.TestConstants.testException
import core.utils.TestModels.testReturnedAddress
import incometax.business.models.address._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.BAD_REQUEST

import scala.concurrent.Future

trait MockAddressLookupConnector extends MockTrait {

  val mockAddressLookupConnector = mock[AddressLookupConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressLookupConnector)
  }

  private def mockInit(request: AddressLookupInitRequest)(response: Future[Either[AddressLookupInitFailureResponse, String]]) =
    when(mockAddressLookupConnector.init(ArgumentMatchers.eq(request))(ArgumentMatchers.any())).thenReturn(response)

  def mockInitSuccess(request: AddressLookupInitRequest)(testRedirectionUrl: String) = mockInit(request)(Right(testRedirectionUrl))

  def mockInitFailure(request: AddressLookupInitRequest) = mockInit(request)(Future.successful(Left(AddressLookupInitFailureResponse(BAD_REQUEST))))

  def mockInitException(request: AddressLookupInitRequest) = mockInit(request)(Future.failed(testException))


  private def mockRetrieveAddress(journeyId: String)(response: Future[Either[ReturnedAddressFailure, ReturnedAddress]]) =
    when(mockAddressLookupConnector.retrieveAddress(ArgumentMatchers.eq(journeyId))(ArgumentMatchers.any())).thenReturn(response)

  def mockRetrieveAddressSuccess(journeyId: String) = mockRetrieveAddress(journeyId)(Right(testReturnedAddress))

  def MockRetrieveAddressFailure(journeyId: String) = mockRetrieveAddress(journeyId)(Left(UnexpectedStatusReturned(BAD_REQUEST)))

  def MockRetrieveAddressException(journeyId: String) = mockRetrieveAddress(journeyId)(Future.failed(testException))

}
