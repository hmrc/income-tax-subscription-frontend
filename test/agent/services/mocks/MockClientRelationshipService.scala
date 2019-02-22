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

package agent.services.mocks

import agent.connectors.mocks.MockAgentServicesConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import agent.services.ClientRelationshipService
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.MockTrait

import scala.concurrent.Future

trait MockClientRelationshipService extends MockTrait {
  val mockClientRelationshipService = mock[ClientRelationshipService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientRelationshipService)
  }

  def preExistingRelationship(arn: String, nino: String)(isPreExistingRelationship: Boolean): Unit =
    when(mockClientRelationshipService.isPreExistingRelationship(arn, nino)).thenReturn(Future.successful(isPreExistingRelationship))

  def preExistingRelationshipFailure(arn: String, nino: String)(failure: Throwable): Unit =
    when(mockClientRelationshipService.isPreExistingRelationship(arn, nino)).thenReturn(Future.failed(failure))

}

trait TestClientRelationshipService extends MockAgentServicesConnector {
  object TestClientRelationshipService extends ClientRelationshipService(mockAgentServicesConnector)
}