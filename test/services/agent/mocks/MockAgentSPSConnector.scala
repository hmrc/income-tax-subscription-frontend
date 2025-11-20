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

import connectors.agent.AgentSPSConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.Future

trait MockAgentSPSConnector extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockAgentSpsConnector: AgentSPSConnector = mock[AgentSPSConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentSpsConnector)
  }

  def verifyAgentSpsConnector(arn: String, utr: String, nino: String, mtditid: String, count: Int = 1): Unit = {
    verify(mockAgentSpsConnector, times(count)).postSpsConfirm(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(mtditid)
    )(ArgumentMatchers.any[HeaderCarrier])
  }

  def mockAgentSpsConnectorSuccess(arn: String, utr: String, nino: String, mtditid: String): Unit = {
    when(mockAgentSpsConnector.postSpsConfirm(ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(mtditid)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
  }

  def mockAgentSpsConnectorFailure(arn: String, utr: String, nino: String, mtditid: String): Unit = {
    when(mockAgentSpsConnector.postSpsConfirm(ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(mtditid)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(Future.failed(new InternalServerException("Test Exception")))
  }
}
