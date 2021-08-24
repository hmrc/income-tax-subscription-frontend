/*
 * Copyright 2021 HM Revenue & Customs
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

package services.agent

import connectors.agent.EnrolmentStoreProxyConnector
import connectors.agent.httpparsers.EnrolmentStoreProxyHttpParser
import models.common.subscription.EnrolmentKey

import javax.inject.{Inject, Singleton}
import services.agent.CheckEnrolmentAllocationService._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckEnrolmentAllocationService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)
                                               (implicit ec: ExecutionContext) {
  def getGroupIdForEnrolment(enrolmentKey: EnrolmentKey)(implicit hc: HeaderCarrier): Future[CheckEnrolmentAllocationResponse] = {
    enrolmentStoreProxyConnector.getAllocatedEnrolments(enrolmentKey) map {
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated) => Right(EnrolmentNotAllocated)
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(groupId)) => Left(EnrolmentAlreadyAllocated(groupId))
      case Left(EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyFailure(status)) => Left(UnexpectedEnrolmentStoreProxyFailure(status))
      case Left(EnrolmentStoreProxyHttpParser.InvalidJsonResponse) => Left(EnrolmentStoreProxyInvalidJsonResponse)
    }
  }
}

object CheckEnrolmentAllocationService {

  type CheckEnrolmentAllocationResponse = Either[CheckEnrolmentAllocationFailure, EnrolmentNotAllocated.type]

  case object EnrolmentNotAllocated

  sealed trait CheckEnrolmentAllocationFailure

  case class EnrolmentAlreadyAllocated(groupId: String) extends CheckEnrolmentAllocationFailure

  case class UnexpectedEnrolmentStoreProxyFailure(status: Int) extends CheckEnrolmentAllocationFailure

  case object EnrolmentStoreProxyInvalidJsonResponse extends CheckEnrolmentAllocationFailure

}

