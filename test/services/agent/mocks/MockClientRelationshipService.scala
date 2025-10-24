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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.agent.ClientRelationshipService

import scala.concurrent.Future

trait MockClientRelationshipService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockClientRelationshipService: ClientRelationshipService = mock[ClientRelationshipService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientRelationshipService)
  }

  def preExistingRelationship(arn: String, nino: String)(isPreExistingRelationship: Boolean): Unit =
    when(mockClientRelationshipService.isPreExistingRelationship(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(isPreExistingRelationship))

  def preExistingRelationshipFailure(arn: String, nino: String)(failure: Throwable): Unit =
    when(mockClientRelationshipService.isPreExistingRelationship(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.failed(failure))

  def preExistingMTDRelationship(arn: String, nino: String)(isPreExistingMTDRelationship: Boolean): Unit =
    when(mockClientRelationshipService.isMTDPreExistingRelationship(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any())).thenReturn(Future.successful(Right(isPreExistingMTDRelationship)))

  def preExistingMTDRelationshipFailure(arn: String, nino: String)(failure: Throwable): Unit =
    when(mockClientRelationshipService.isMTDPreExistingRelationship(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any())).thenReturn(Future.failed(failure))

  def suppAgentRelationship(arn: String, nino: String)(isMTDSuppAgentRelationship: Boolean): Unit =
    when(mockClientRelationshipService.isMTDSuppAgentRelationship(
      ArgumentMatchers.eq(arn),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(isMTDSuppAgentRelationship)))


  def verifyCheckPreExistingMTDRelationship(arn: String, nino: String, count: Int = 1): Unit = {
    verify(mockClientRelationshipService, times(count))
      .isMTDPreExistingRelationship(ArgumentMatchers.eq(arn), ArgumentMatchers.eq(nino))(ArgumentMatchers.any())
  }

  def verifyCheckMTDSuppAgentRelationship(arn: String, nino: String, count: Int = 1): Unit = {
    verify(mockClientRelationshipService, times(count))
      .isMTDSuppAgentRelationship(ArgumentMatchers.eq(arn), ArgumentMatchers.eq(nino))(ArgumentMatchers.any())
  }
}

trait TestClientRelationshipService extends MockAgentServicesConnector {
  object TestClientRelationshipService extends ClientRelationshipService(mockAgentServicesConnector)
}
