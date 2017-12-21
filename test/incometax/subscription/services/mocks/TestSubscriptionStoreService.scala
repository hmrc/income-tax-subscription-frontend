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

package incometax.subscription.services.mocks

import core.config.MockConfig
import core.services.mocks.MockKeystoreService
import core.utils.MockTrait
import incometax.subscription.connectors.mocks.MockSubscriptionStoreConnector
import incometax.subscription.models.StoredSubscription
import incometax.subscription.services.SubscriptionStoreService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.TestModels._

import scala.concurrent.Future

trait MockSubscriptionStoreService extends MockTrait {
  val mockSubscriptionStoreService = mock[SubscriptionStoreService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionStoreService)
  }

  def mockRetrieveSubscriptionData(nino: String)(result: Future[Option[StoredSubscription]]): Unit = {
    when(mockSubscriptionStoreService.retrieveSubscriptionData(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }

  val successfulStoredSubscriptionFound = Future(Some(testStoredSubscription))

  val successfulSubscriptionNotFound = Future(None)
}

trait TestSubscriptionStoreService extends MockSubscriptionStoreConnector with MockKeystoreService with MockConfig {
  object TestSubscriptionStoreService extends SubscriptionStoreService(
    subscriptionStoreConnector = mockSubscriptionStoreConnector,
    keystoreService = MockKeystoreService,
    appConfig = new MockConfig {
      override val unauthorisedAgentEnabled = true
    }
  )

  object TestSubscriptionStoreServiceDisabled extends SubscriptionStoreService(
    subscriptionStoreConnector = mockSubscriptionStoreConnector,
    keystoreService = MockKeystoreService,
    appConfig = MockConfig
  )
}
