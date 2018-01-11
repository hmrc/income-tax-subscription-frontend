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

package incometax.unauthorisedagent.connectors.mocks

import core.utils.MockTrait
import core.utils.TestConstants._
import core.utils.TestModels._
import incometax.unauthorisedagent.connectors.SubscriptionStoreConnector
import incometax.unauthorisedagent.httpparsers.DeleteSubscriptionResponseHttpParser.DeleteSubscriptionResponse
import incometax.unauthorisedagent.httpparsers.RetrieveSubscriptionResponseHttpParser.RetrieveSubscriptionResponse
import incometax.unauthorisedagent.httpparsers.StoreSubscriptionResponseHttpParser.StoreSubscriptionResponse
import incometax.unauthorisedagent.models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubscriptionStoreConnector extends MockTrait {
  val mockSubscriptionStoreConnector = mock[SubscriptionStoreConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionStoreConnector)
  }

  def mockStoreSubscriptionData(nino: String, subscriptionData: StoredSubscription)(response: Future[StoreSubscriptionResponse]): Unit = {
    when(mockSubscriptionStoreConnector.storeSubscriptionData(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(subscriptionData))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)
  }

  def mockStoreSubscriptionDataSuccess(nino: String, subscriptionData: StoredSubscription): Unit =
    mockStoreSubscriptionData(nino = nino, subscriptionData = subscriptionData)(Right(StoreSubscriptionSuccess))

  def mockStoreSubscriptionDataFailure(nino: String, subscriptionData: StoredSubscription): Unit =
    mockStoreSubscriptionData(nino = nino, subscriptionData = subscriptionData)(Left(StoreSubscriptionFailure(testErrorMessage)))

  def mockRetrieveSubscriptionData(nino: String)(response: Future[RetrieveSubscriptionResponse]): Unit = {
    when(mockSubscriptionStoreConnector.retrieveSubscriptionData(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)
  }

  val successfulRetrieveSubscriptionResponse: Future[RetrieveSubscriptionResponse] =
    Future.successful(Right(Some(testStoredSubscription)))

  val successfulSubscriptionNotFound: Future[RetrieveSubscriptionResponse] =
    Future.successful(Right(None))

  val retrieveSubscriptionFailure: Future[RetrieveSubscriptionResponse] =
    Future.successful(Left(RetrieveSubscriptionFailure(testErrorMessage)))

  def mockDeleteSubscriptionData(nino: String)(response: Future[DeleteSubscriptionResponse]): Unit = {
    when(mockSubscriptionStoreConnector.deleteSubscriptionData(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)
  }

  val deleteSubscriptionSuccess: Future[DeleteSubscriptionResponse] =
    Future.successful(Right(DeleteSubscriptionSuccess))

  val deleteSubscriptionFailure: Future[DeleteSubscriptionResponse] =
    Future.successful(Left(DeleteSubscriptionFailure(testErrorMessage)))
}
