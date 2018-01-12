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

package incometax.unauthorisedagent.services.mocks

import agent.services.mocks.MockKeystoreService
import core.config.MockConfig
import core.utils.MockTrait
import core.utils.TestConstants._
import incometax.unauthorisedagent.connectors.mocks.MockSubscriptionStoreConnector
import incometax.unauthorisedagent.httpparsers.StoreSubscriptionResponseHttpParser.StoreSubscriptionResponse
import incometax.unauthorisedagent.models.{StoreSubscriptionFailure, StoreSubscriptionSuccess}
import incometax.unauthorisedagent.services.SubscriptionStorePersistenceService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubscriptionStorePersistenceService extends MockTrait {
  val mockSubscriptionStorePersistenceService = mock[SubscriptionStorePersistenceService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionStorePersistenceService)
  }

  def mockStoreSubscription(arn: String, nino: String)(result: Future[StoreSubscriptionResponse]): Unit = {
    when(mockSubscriptionStorePersistenceService.storeSubscription(ArgumentMatchers.eq(arn), ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }
  val successfulStoredSubscription = Future(Right(StoreSubscriptionSuccess))

  val failureStoredSubscription = Future(Left(StoreSubscriptionFailure(testErrorMessage)))

  def mockStoredSubscriptionSuccess(arn: String, nino: String): Unit = mockStoreSubscription(arn, nino)(successfulStoredSubscription)

  def mockStoredSubscriptionFailure(arn: String, nino: String): Unit = mockStoreSubscription(arn, nino)(failureStoredSubscription)

}

trait TestSubscriptionStorePersistenceService extends MockSubscriptionStoreConnector with MockKeystoreService with MockConfig {

  object TestSubscriptionStorePersistenceService extends SubscriptionStorePersistenceService(
    subscriptionStoreConnector = mockSubscriptionStoreConnector,
    keystoreService = MockKeystoreService,
    appConfig = MockConfig
  )

}
