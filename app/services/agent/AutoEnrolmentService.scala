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

package services.agent

import cats.data.EitherT
import cats.implicits._
import common.Constants.{utrEnrolmentIdentifierKey, utrEnrolmentName}
import connectors.agent.httpparsers.GetUsersForGroupHttpParser.UsersFound
import connectors.agent.httpparsers.{AllocateEnrolmentResponseHttpParser, QueryUsersHttpParser, UpsertEnrolmentResponseHttpParser}
import connectors.agent.{EnrolmentStoreProxyConnector, UsersGroupsSearchConnector}
import models.ConnectorError
import models.common.subscription.EnrolmentKey
import play.api.Logging
import services.agent.AutoEnrolmentService._
import uk.gov.hmrc.auth.core.User
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AutoEnrolmentService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                     checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                     assignEnrolmentToUserService: AssignEnrolmentToUserService,
                                     usersGroupsSearchConnector: UsersGroupsSearchConnector)
                                    (implicit ec: ExecutionContext) extends Logging {

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.warn(s"[AutoEnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  def autoClaimEnrolment(utr: String, nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[AutoClaimEnrolmentResponse] = {
    for {
      _ <- upsertEnrolmentAllocation(mtditid = mtditid, nino = nino)
      groupId <- getEnrolmentAllocation(utr = utr, nino = nino)
      enrolmentUserIDs <- getEnrolmentUserIDs(utr = utr, nino = nino)
      adminUserId <- getAdminUserId(groupId = groupId, enrolmentUserIDs = enrolmentUserIDs, nino = nino)
      _ <- allocateEnrolmentWithoutKnownFacts(mtditid = mtditid, groupId = groupId, credentialId = adminUserId, nino = nino)
      _ <- assignEnrolmentToUser(enrolmentUserIDs filterNot (_ == adminUserId), mtditid = mtditid, nino = nino)
    } yield {
      logger.debug(s"[AutoEnrolmentService][autoClaimEnrolment] - Successful auto enrolment for nino: $nino")
      EnrolmentAssigned
    }
  }.value

  private def getEnrolmentAllocation(utr: String, nino: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, String] = {

    val functionError: String => Unit = logError("getEnrolmentAllocation", nino, _)

    EitherT(checkEnrolmentAllocationService.getGroupIdForEnrolment(EnrolmentKey(utrEnrolmentName, utrEnrolmentIdentifierKey -> utr))) transform {
      case Right(_) =>
        functionError("Enrolment not allocated")
        Left(EnrolmentNotAllocated)
      case Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(groupId)) =>
        Right(groupId)
      case Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(status)) =>
        functionError(s"Enrolment store proxy call failed with status: $status")
        Left(EnrolmentStoreProxyFailure(status))
      case Left(CheckEnrolmentAllocationService.EnrolmentStoreProxyInvalidJsonResponse) =>
        functionError("Enrolment store proxy invalid json")
        Left(EnrolmentStoreProxyInvalidJsonResponse)
    }
  }

  private def getEnrolmentUserIDs(utr: String, nino: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, Set[String]] = {

    val functionError: String => Unit = logError("getEnrolmentUserIDs", nino, _)

    EitherT(enrolmentStoreProxyConnector.getUserIds(utr)) transform {
      case Right(QueryUsersHttpParser.UsersFound(retrievedUserIds)) if retrievedUserIds.nonEmpty =>
        Right(retrievedUserIds)
      case Right(QueryUsersHttpParser.NoUsersFound) =>
        functionError("No users found")
        Left(NoUsersFound)
      case _ =>
        functionError("Enrolment store proxy failure")
        Left(EnrolmentStoreProxyConnectionFailure)
    }
  }

  private def getAdminUserId(groupId: String, enrolmentUserIDs: Set[String], nino: String)
                            (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, String] = {

    val functionError: String => Unit = logError("getAdminUserId", nino, _)

    EitherT(usersGroupsSearchConnector.getUsersForGroup(groupId)) transform {
      case Right(UsersFound(userIds)) =>
        userIds collectFirst {
          case (userId, User) if enrolmentUserIDs contains userId => userId
        } match {
          case Some(userId) => Right(userId)
          case None =>
            functionError("No users found in group")
            Left(NoAdminUsers)
        }
      case Left(_) =>
        functionError("Users groups search failure")
        Left(UsersGroupSearchFailure)
    }
  }

  private def upsertEnrolmentAllocation(mtditid: String, nino: String)
                                       (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] = {

    val functionError: String => Unit = logError("upsertEnrolmentAllocation", nino, _)

    EitherT(enrolmentStoreProxyConnector.upsertEnrolment(mtditid = mtditid, nino = nino)) transform {
      case Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess) =>
        Right(AutoEnrolmentService.UpsertEnrolmentSuccess)
      case Left(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure(status, message)) =>
        functionError(s"Failed to upsert enrolment with status: $status, message: $message")
        Left(AutoEnrolmentService.UpsertEnrolmentFailure(message))
    }
  }

  private def allocateEnrolmentWithoutKnownFacts(mtditid: String, groupId: String, credentialId: String, nino: String)
                                                (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] = {

    val functionError: String => Unit = logError("allocateEnrolmentWithoutKnownFacts", nino, _)

    EitherT(enrolmentStoreProxyConnector.allocateEnrolmentWithoutKnownFacts(groupId = groupId, credentialId = credentialId, mtditid = mtditid)) transform {
      case Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess) =>
        Right(AutoEnrolmentService.EnrolSuccess)
      case Left(AllocateEnrolmentResponseHttpParser.EnrolFailure(message)) =>
        functionError(s"Failed to allocate enrolment to group, message: $message")
        Left(AutoEnrolmentService.EnrolAdminIdFailure(credentialId, message))
    }
  }

  private def assignEnrolmentToUser(credentialIds: Set[String], mtditid: String, nino: String)
                                   (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] = {

    val functionError: String => Unit = logError("assignEnrolmentToUser", nino, _)

    EitherT(assignEnrolmentToUserService.assignEnrolment(userIds = credentialIds, mtditid = mtditid)) transform {
      case Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers) =>
        Right(AutoEnrolmentService.EnrolmentAssigned)
      case Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed(failedIds)) =>
        functionError(s"Failed to assign enrolment to users, ${failedIds.size} out of ${credentialIds.size} failed")
        Left(AutoEnrolmentService.EnrolmentAssignmentFailureForIds(failedIds))
    }
  }

}

object AutoEnrolmentService {

  type AutoClaimEnrolmentResponse = Either[AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess]

  sealed trait AutoClaimEnrolmentSuccess

  case object EnrolSuccess extends AutoClaimEnrolmentSuccess

  case object EnrolmentAssigned extends AutoClaimEnrolmentSuccess

  case object UpsertEnrolmentSuccess extends AutoClaimEnrolmentSuccess

  sealed trait AutoClaimEnrolmentFailure extends ConnectorError

  case object NoUsersFound extends AutoClaimEnrolmentFailure

  case class EnrolAdminIdFailure(adminId: String, message: String) extends AutoClaimEnrolmentFailure

  case object EnrolmentNotAllocated extends AutoClaimEnrolmentFailure

  case class EnrolmentStoreProxyFailure(status: Int) extends AutoClaimEnrolmentFailure

  case object EnrolmentStoreProxyInvalidJsonResponse extends AutoClaimEnrolmentFailure

  case object EnrolmentStoreProxyConnectionFailure extends AutoClaimEnrolmentFailure

  case class UpsertEnrolmentFailure(failureMessage: String) extends AutoClaimEnrolmentFailure

  case class EnrolmentAssignmentFailureForIds(failedIds: Set[String]) extends AutoClaimEnrolmentFailure

  case object UsersGroupSearchFailure extends AutoClaimEnrolmentFailure

  case object NoAdminUsers extends AutoClaimEnrolmentFailure

}
