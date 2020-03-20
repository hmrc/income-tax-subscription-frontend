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

package connectors.agent.mocks

import connectors.agent.AgentServicesConnector
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import utilities.MockTrait

import scala.concurrent.Future

trait TestAgentServicesConnector extends MockHttp {

  object TestAgentServicesConnector extends AgentServicesConnector(appConfig, mockHttp)

  def mockIsPreExistingRelationship(arn: String, nino: String)(status: Int, response: Option[JsValue] = None): Unit =
    setupMockHttpGet(Some(TestAgentServicesConnector.agentClientURL(arn, nino)))(status, response)
}

trait MockAgentServicesConnector extends MockTrait {

  val mockAgentServicesConnector: AgentServicesConnector = mock[AgentServicesConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentServicesConnector)
  }

  def preExistingRelationship(arn: String, nino: String)(isPreExistingRelationship: Boolean): Unit =
    when(mockAgentServicesConnector.isPreExistingRelationship(arn, nino)).thenReturn(Future.successful(isPreExistingRelationship))

  def preExistingRelationshipFailure(arn: String, nino: String)(failure: Throwable): Unit =
    when(mockAgentServicesConnector.isPreExistingRelationship(arn, nino)).thenReturn(Future.failed(failure))

}
