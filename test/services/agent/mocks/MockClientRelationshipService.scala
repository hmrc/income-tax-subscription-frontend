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

package services.agent.mocks

import connectors.agent.mocks.MockAgentServicesConnector
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.agent.ClientRelationshipService
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockClientRelationshipService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {
  val mockClientRelationshipService: ClientRelationshipService = mock[ClientRelationshipService]

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
