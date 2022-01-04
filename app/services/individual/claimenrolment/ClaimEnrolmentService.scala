/*
 * Copyright 2022 HM Revenue & Customs
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

package services.individual.claimenrolment

import auth.individual.IncomeTaxSAUser
import cats.data.EitherT
import cats.implicits._
import models.common.subscription.EnrolmentKey
import play.api.mvc.{AnyContent, Request}
import services.SubscriptionService
import services.agent.CheckEnrolmentAllocationService
import services.agent.CheckEnrolmentAllocationService.{EnrolmentAlreadyAllocated, EnrolmentStoreProxyInvalidJsonResponse, UnexpectedEnrolmentStoreProxyFailure}
import services.individual.claimenrolment.ClaimEnrolmentService._
import services.individual.{EnrolmentService, KnownFactsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.Constants.{mtdItsaEnrolmentIdentifierKey, mtdItsaEnrolmentName}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimEnrolmentService @Inject()(subscriptionService: SubscriptionService,
                                      checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                      knownFactsService: KnownFactsService,
                                      enrolmentService: EnrolmentService)(implicit ec: ExecutionContext) {

  def claimEnrolment(implicit request: Request[AnyContent], user: IncomeTaxSAUser, hc: HeaderCarrier): Future[ClaimEnrolmentResponse] = {
    val claimEnrolmentResult = for {
      nino <- EitherT(getNino)
      mtditid <- EitherT(getMtditid(nino))
      _ <- EitherT(getEnrolmentAllocation(nino, mtditid))
      _ <- EitherT(addKnownFacts(nino, mtditid))
      allocationResult <- EitherT(allocateEnrolment(nino, mtditid))
    } yield allocationResult

    claimEnrolmentResult.value
  }

  def getMtditidFromSubscription(implicit request: Request[AnyContent], user: IncomeTaxSAUser,
                                 hc: HeaderCarrier): Future[Either[ClaimEnrolmentFailure, String]] = {
    val mtdIdFromSub = for {
      nino <- EitherT(getNino)
      mtditid <- EitherT(getMtditid(nino))
    } yield mtditid

    mtdIdFromSub.value

  }

  private def getNino(implicit request: Request[AnyContent], user: IncomeTaxSAUser): Future[Either[ClaimEnrolmentFailure, String]] = {
    user.nino match {
      case Some(nino) =>
        Future.successful(Right(nino))
      case None =>
        Future.successful(Left(ClaimEnrolmentError("[ClaimEnrolmentService][getNino] - Could not retrieve nino from user profile")))
    }
  }

  private def getMtditid(nino: String)(implicit hc: HeaderCarrier): Future[Either[ClaimEnrolmentFailure, String]] = {
    subscriptionService.getSubscription(nino) map {
      case Right(Some(subscriptionResponse)) =>
        Right(subscriptionResponse.mtditId)
      case Right(None) =>
        Left(NotSubscribed)
      case Left(failureResponse) =>
        Left(ClaimEnrolmentError(s"[ClaimEnrolmentService][getMtditid] - Unexpected response calling get business details. Status: ${failureResponse.status}"))
    }
  }

  private def getEnrolmentAllocation(nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[ClaimEnrolmentResponse] = {
    val enrolmentKey: EnrolmentKey = EnrolmentKey(mtdItsaEnrolmentName, mtdItsaEnrolmentIdentifierKey -> mtditid)
    checkEnrolmentAllocationService.getGroupIdForEnrolment(enrolmentKey) map {
      case Right(_) =>
        Right(ClaimEnrolmentSuccess(nino, mtditid))
      case Left(EnrolmentAlreadyAllocated(_)) =>
        Left(AlreadySignedUp)
      case Left(EnrolmentStoreProxyInvalidJsonResponse) =>
        Left(ClaimEnrolmentError("[ClaimEnrolmentService][getEnrolmentAllocation] - Unable to parse response"))
      case Left(UnexpectedEnrolmentStoreProxyFailure(status)) =>
        Left(ClaimEnrolmentError(s"[ClaimEnrolmentService][getEnrolmentAllocation] - Unexpected response. Status: $status"))
    }
  }

  private def addKnownFacts(nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[ClaimEnrolmentResponse] = {
    knownFactsService.addKnownFacts(mtditId = mtditid, nino = nino) map {
      case Right(_) =>
        Right(ClaimEnrolmentSuccess(nino, mtditid))
      case Left(response) =>
        Left(ClaimEnrolmentError(s"[ClaimEnrolmentService][addKnownFacts] - Unexpected response whilst adding known facts. Response: ${response.message}"))
    }
  }

  private def allocateEnrolment(nino: String, mtditid: String)(implicit hc: HeaderCarrier): Future[ClaimEnrolmentResponse] = {
    enrolmentService.enrol(mtditId = mtditid, nino = nino) map {
      case Right(_) =>
        Right(ClaimEnrolmentSuccess(nino, mtditid))
      case Left(response) =>
        Left(ClaimEnrolmentError(s"[ClaimEnrolmentService][allocateEnrolment] - Unexpected response whist allocating enrolment. Response: ${response.message}"))
    }
  }

}

object ClaimEnrolmentService {

  type ClaimEnrolmentResponse = Either[ClaimEnrolmentFailure, ClaimEnrolmentSuccess]

  case class ClaimEnrolmentSuccess(nino: String, mtditid: String)

  sealed trait ClaimEnrolmentFailure

  case object NotSubscribed extends ClaimEnrolmentFailure

  case object AlreadySignedUp extends ClaimEnrolmentFailure

  case class ClaimEnrolmentError(msg: String) extends ClaimEnrolmentFailure

}
