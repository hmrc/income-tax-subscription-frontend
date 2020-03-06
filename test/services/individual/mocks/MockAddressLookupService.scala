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

package services.individual.mocks

import connectors.individual.business.httpparsers.AddressLookupResponseHttpParser.{ConfirmAddressLookupResponseResponse, InitAddressLookupResponseResponse}
import connectors.individual.business.mocks.MockAddressLookupConnector
import core.utils.MockTrait
import core.utils.TestConstants.testException
import core.utils.TestModels.testReturnedAddress
import models.individual.business.address._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.BAD_REQUEST
import services.individual.AddressLookupService

import scala.concurrent.Future


trait MockAddressLookupService extends MockTrait {

  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressLookupService)
  }

  private def mockInit(request: AddressLookupInitRequest)(response: Future[Either[AddressLookupInitFailureResponse, String]]) =
    when(mockAddressLookupService.init(ArgumentMatchers.eq(request))(ArgumentMatchers.any())).thenReturn(response)

  def mockInitSuccess(request: AddressLookupInitRequest)(testRedirectionUrl: String): OngoingStubbing[Future[InitAddressLookupResponseResponse]] =
    mockInit(request)(Right(testRedirectionUrl))

  def mockInitFailure(request: AddressLookupInitRequest): OngoingStubbing[Future[InitAddressLookupResponseResponse]] =
    mockInit(request)(Future.successful(Left(AddressLookupInitFailureResponse(BAD_REQUEST))))

  def mockInitException(request: AddressLookupInitRequest): OngoingStubbing[Future[InitAddressLookupResponseResponse]] =
    mockInit(request)(Future.failed(testException))

  private def mockRetrieveAddress(journeyId: String)(response: Future[Either[ReturnedAddressFailure, ReturnedAddress]]) =
    when(mockAddressLookupService.retrieveAddress(ArgumentMatchers.eq(journeyId))(ArgumentMatchers.any())).thenReturn(response)

  def mockRetrieveAddressSuccess(journeyId: String): OngoingStubbing[Future[ConfirmAddressLookupResponseResponse]] =
    mockRetrieveAddress(journeyId)(Right(testReturnedAddress))

  def mockRetrieveAddressNoneUK(journeyId: String): OngoingStubbing[Future[ConfirmAddressLookupResponseResponse]] =
    mockRetrieveAddress(journeyId)(
      Right(testReturnedAddress.copy(address =
        testReturnedAddress.address.copy(country = Some(Country("NOTUK", "NOTUK"))))
      )
    )

  def MockRetrieveAddressFailure(journeyId: String): OngoingStubbing[Future[ConfirmAddressLookupResponseResponse]] =
    mockRetrieveAddress(journeyId)(Left(UnexpectedStatusReturned(BAD_REQUEST)))

  def MockRetrieveAddressException(journeyId: String): OngoingStubbing[Future[ConfirmAddressLookupResponseResponse]] =
    mockRetrieveAddress(journeyId)(Future.failed(testException))

}


trait TestAddressLookupService extends MockAddressLookupConnector {

  object TestAddressLookupService extends AddressLookupService(
    mockAddressLookupConnector
  )

}
