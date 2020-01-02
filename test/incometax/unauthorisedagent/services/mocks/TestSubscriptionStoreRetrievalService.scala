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

package incometax.unauthorisedagent.services.mocks

import core.config.MockConfig
import core.services.mocks.MockKeystoreService
import core.utils.MockTrait
import core.utils.TestModels._
import incometax.unauthorisedagent.services.SubscriptionStoreRetrievalService
import incometax.unauthorisedagent.connectors.mocks.MockSubscriptionStoreConnector
import incometax.unauthorisedagent.models.{DeleteSubscriptionSuccess, StoredSubscription}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubscriptionStoreRetrievalService extends MockTrait {
  val mockSubscriptionStoreRetrievalService = mock[SubscriptionStoreRetrievalService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionStoreRetrievalService)
  }

  def mockRetrieveSubscriptionData(nino: String)(result: Future[Option[StoredSubscription]]): Unit = {
    when(mockSubscriptionStoreRetrievalService.retrieveSubscriptionData(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }

  val successfulStoredSubscriptionFound = Future(Some(testStoredSubscription))

  val successfulSubscriptionNotFound = Future(None)

  def mockDeleteSubscriptionData(nino:String): Unit = {
    when(mockSubscriptionStoreRetrievalService.deleteSubscriptionData(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(Future.successful(DeleteSubscriptionSuccess))
  }
}

trait TestSubscriptionStoreRetrievalService extends MockSubscriptionStoreConnector with MockKeystoreService with MockConfig {
  object TestSubscriptionStoreRetrievalService extends SubscriptionStoreRetrievalService(
    subscriptionStoreConnector = mockSubscriptionStoreConnector,
    keystoreService = MockKeystoreService,
    appConfig = new MockConfig {
      override val unauthorisedAgentEnabled = true
    }
  )

  object TestSubscriptionStoreRetrievalServiceDisabled extends SubscriptionStoreRetrievalService(
    subscriptionStoreConnector = mockSubscriptionStoreConnector,
    keystoreService = MockKeystoreService,
    appConfig = MockConfig
  )
}
