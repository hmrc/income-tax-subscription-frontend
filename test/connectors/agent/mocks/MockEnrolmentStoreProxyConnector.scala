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

package connectors.agent.mocks

import connectors.agent.EnrolmentStoreProxyConnector
import connectors.agent.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import connectors.agent.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import connectors.agent.httpparsers.QueryUsersHttpParser.QueryUsersResponse
import connectors.agent.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse
import models.common.subscription.EnrolmentKey
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockEnrolmentStoreProxyConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEnrolmentStoreProxyConnector)
  }

  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]

  def mockGetAllocatedEnrolment(enrolmentKey: EnrolmentKey)(response: EnrolmentStoreProxyResponse): Unit = {
    when(mockEnrolmentStoreProxyConnector.getAllocatedEnrolments(
      ArgumentMatchers.eq(enrolmentKey)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)
  }

  def mockGetUserIds(utr: String)(response: QueryUsersResponse): Unit = {
    when(mockEnrolmentStoreProxyConnector.getUserIds(
      ArgumentMatchers.eq(utr)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)
  }

  def mockAllocateEnrolmentWithoutKnownFacts(groupId: String,
                                             credentialId: String,
                                             mtdId: String)(response: AllocateEnrolmentResponse): Unit =
    when(mockEnrolmentStoreProxyConnector.allocateEnrolmentWithoutKnownFacts(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(credentialId),
      ArgumentMatchers.eq(mtdId)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)

  def mockAssignEnrolment(userId: String, mtdId: String)(response: AssignEnrolmentToUserResponse): Unit =
    when(mockEnrolmentStoreProxyConnector.assignEnrolment(
      ArgumentMatchers.eq(userId),
      ArgumentMatchers.eq(mtdId)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)


  def mockEnrolmentStoreUpsertEnrolment(mtdId: String,
                                        nino: String)(response: UpsertEnrolmentResponse): Unit = {
    when(mockEnrolmentStoreProxyConnector.upsertEnrolment(
      ArgumentMatchers.eq(mtdId),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)
  }
}

