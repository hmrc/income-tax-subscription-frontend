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

package connectors.subscriptiondata.mocks

import connectors.IncomeTaxSubscriptionConnector
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.{DeleteSubscriptionDetailsResponse, UnexpectedStatusFailure}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockIncomeTaxSubscriptionConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockIncomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeTaxSubscriptionConnector)
  }

  def mockGetSubscriptionDetails[T](id: String)(response: Option[T]): OngoingStubbing[Future[Option[T]]] = {
    when(mockIncomeTaxSubscriptionConnector.getSubscriptionDetails[T](
      ArgumentMatchers.any(), ArgumentMatchers.eq(id)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

  def mockGetSubscriptionDetailsSeq[T](id: String)(response: Seq[T]): OngoingStubbing[Future[Seq[T]]] = {
    when(mockIncomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[T](
      ArgumentMatchers.any(), ArgumentMatchers.eq(id)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

  def mockSaveSubscriptionDetails[T](id: String, value: T)
                                    (response: PostSubscriptionDetailsResponse): OngoingStubbing[Future[PostSubscriptionDetailsResponse]] = {
    when(mockIncomeTaxSubscriptionConnector.saveSubscriptionDetails[T](
      ArgumentMatchers.any(), ArgumentMatchers.eq(id), ArgumentMatchers.eq(value)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

  def mockDeleteSubscriptionDetails(id: String)(response: DeleteSubscriptionDetailsResponse): Unit = {
    when(mockIncomeTaxSubscriptionConnector.deleteSubscriptionDetails(
      ArgumentMatchers.any(), ArgumentMatchers.eq(id)
    )(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  def mockDeleteSubscriptionDetailsFailure(id: String): Unit = {
    when(mockIncomeTaxSubscriptionConnector.deleteSubscriptionDetails(
      ArgumentMatchers.any(), ArgumentMatchers.eq(id)
    )(ArgumentMatchers.any())) thenReturn Future.successful(Left(UnexpectedStatusFailure(500)))
  }

  def verifyDeleteSubscriptionDetails(id: String, count: Int): Unit = {
    verify(mockIncomeTaxSubscriptionConnector, times(count))
      .deleteSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.eq(id))(ArgumentMatchers.any())

  }

}
