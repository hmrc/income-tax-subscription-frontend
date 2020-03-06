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

package services.agent

import connectors.agent.EnrolmentStoreProxyConnector
import javax.inject.{Inject, Singleton}
import services.agent.AssignEnrolmentToUserService.{EnrolmentAssignedToUsers, EnrolmentAssignmentFailed, EnrolmentAssignmentResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssignEnrolmentToUserService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)
                                            (implicit ec: ExecutionContext) {

  def assignEnrolment(userIds: Set[String], mtditid: String)(implicit hc: HeaderCarrier): Future[EnrolmentAssignmentResponse] = {
    Future.sequence {
      userIds.map { userId =>
        enrolmentStoreProxyConnector.assignEnrolment(userId, mtditid)
      }
    } map { userIdResponses =>
      if (userIdResponses.forall(_.isRight)) {
        Right(EnrolmentAssignedToUsers)
      } else {
        val failedUserIds: Set[String] = (userIds zip userIdResponses).collect {
          case (userId, response) if response.isLeft => userId
        }
        Left(EnrolmentAssignmentFailed(failedUserIds))
      }
    }

  }

}

object AssignEnrolmentToUserService {

  type EnrolmentAssignmentResponse = Either[EnrolmentAssignmentFailed, EnrolmentAssignedToUsers.type]

  case object EnrolmentAssignedToUsers

  case class EnrolmentAssignmentFailed(failedIds: Set[String])

}
