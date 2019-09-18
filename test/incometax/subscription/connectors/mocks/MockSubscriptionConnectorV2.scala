/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.subscription.connectors.mocks

import core.utils.MockTrait
import incometax.subscription.connectors.SubscriptionConnectorV2
import incometax.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import incometax.subscription.models.SubscriptionRequestV2
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubscriptionConnectorV2 extends MockTrait {

  val mockSubscriptionConnectorV2: SubscriptionConnectorV2 = mock[SubscriptionConnectorV2]

  def setupMockSubscriptionV2(request: SubscriptionRequestV2)(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionConnectorV2.subscribe(ArgumentMatchers.eq(request))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

}
