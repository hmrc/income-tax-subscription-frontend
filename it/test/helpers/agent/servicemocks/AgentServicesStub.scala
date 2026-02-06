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

package helpers.agent.servicemocks

import connectors.agent.AgentServicesConnector
import play.api.http.Status

object AgentServicesStub extends WireMockMethods {

  def stubClientRelationship(arn: String, nino: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.agentClientURI(arn, nino))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }

  def stubClientRelationship(arn: String, nino: String)(status: Int): Unit = {
    when(method = GET, uri = AgentServicesConnector.agentClientURI(arn, nino))
      .thenReturn(status = status)
  }

  def stubMTDClientRelationship(arn: String, nino: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.agentMTDClientURI(arn, nino))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }

  def stubMTDClientRelationship(arn: String, nino: String)(status: Int): Unit = {
    when(method = GET, uri = AgentServicesConnector.agentMTDClientURI(arn, nino))
      .thenReturn(status = status)
  }

  def stubMTDSuppAgentRelationship(arn: String, nino: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.suppAgentClientURI(arn, nino))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }

  def stubMTDSuppAgentRelationship(arn: String, nino: String)(status: Int): Unit = {

    when(method = GET, uri = AgentServicesConnector.suppAgentClientURI(arn, nino))
      .thenReturn(status = status)
  }

  def stubMTDRelationship(arn: String, mtdId: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.agentClientMtditidURI(arn, mtdId))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }

  def stubMTDRelationship(arn: String, mtdId: String)(status: Int): Unit = {
    when(method = GET, uri = AgentServicesConnector.agentClientMtditidURI(arn, mtdId))
      .thenReturn(status = status)
  }

  def stubMTDSuppRelationship(arn: String, mtdId: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.suppAgentClientMtditidURI(arn, mtdId))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }

  def stubMTDSuppRelationship(arn: String, mtdId: String)(status: Int): Unit = {
    when(method = GET, uri = AgentServicesConnector.suppAgentClientMtditidURI(arn, mtdId))
      .thenReturn(status = status)
  }


}
