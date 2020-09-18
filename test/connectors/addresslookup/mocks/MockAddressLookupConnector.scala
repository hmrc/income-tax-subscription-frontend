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

package connectors.addresslookup.mocks

import connectors.AddressLookupConnector
import connectors.httpparser.addresslookup.GetAddressLookupDetailsHttpParser.GetAddressLookupDetailsResponse
import connectors.httpparser.addresslookup.PostAddressLookupHttpParser.PostAddressLookupResponse
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockAddressLookupConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAddressLookupConnector)
  }

  def mockGetAddressDetails(id: String)(response: GetAddressLookupDetailsResponse): OngoingStubbing[Future[GetAddressLookupDetailsResponse]] = {
    when(mockAddressLookupConnector.getAddressDetails(
      ArgumentMatchers.eq(id))(ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

  def mockInitialiseAddressLookup(continueUrl: String)
                                 (response: PostAddressLookupResponse): OngoingStubbing[Future[PostAddressLookupResponse]] = {
    when(mockAddressLookupConnector.initialiseAddressLookup(
      ArgumentMatchers.eq(continueUrl))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }
}
