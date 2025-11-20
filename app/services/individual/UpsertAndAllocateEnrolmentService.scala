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

package services.individual

import cats.data.EitherT
import common.Constants
import common.Constants.GovernmentGateway._
import connectors.individual.TaxEnrolmentsConnector
import models.common.subscription.{EmacEnrolmentRequest, EnrolmentKey, EnrolmentVerifiers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpsertAndAllocateEnrolmentService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                                  authConnector: AuthConnector)(implicit ec: ExecutionContext) {

  import services.individual.UpsertAndAllocateEnrolmentService._
  def upsertAndAllocate(mtditid: String, nino: String)(implicit hc: HeaderCarrier): Future[UpsertAndAllocateEnrolmentResponse] = {
    val enrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> mtditid)

    val upsertAndAllocateResult = for {
      _ <- EitherT(upsertEnrolment(enrolmentKey, nino))
      result <- EitherT(allocateEnrolment(enrolmentKey, nino))
    } yield {
      result
    }

    upsertAndAllocateResult.value

  }

  private def upsertEnrolment(enrolmentKey: EnrolmentKey, nino: String)(implicit hc: HeaderCarrier) = {
    val enrolmentVerifiers = EnrolmentVerifiers(NINO -> nino)
    taxEnrolmentsConnector.upsertEnrolment(enrolmentKey, enrolmentVerifiers) map {
      case Right(value) => Right(value)
      case Left(_) => Left(UpsertKnownFactsFailure)
    }
  }

  private def allocateEnrolment(enrolmentKey: EnrolmentKey, nino: String)(implicit hc: HeaderCarrier) = {
    authConnector.authorise(EmptyPredicate, credentials and groupIdentifier) flatMap {
      case Some(Credentials(credId, _)) ~ Some(groupId) =>
        val enrolmentRequest = EmacEnrolmentRequest(credId, nino)
        taxEnrolmentsConnector.allocateEnrolment(groupId, enrolmentKey, enrolmentRequest) map {
          case Right(_) => Right(UpsertAndAllocateEnrolmentSuccess)
          case Left(_) => Left(AllocateEnrolmentFailure)
        }
      case Some(_) ~ None =>
        Future.successful(Left(NoGroupIdFailure))
      case _ ~ _ =>
        Future.successful(Left(NoCredentialsFailure))
    }
  }

}

object UpsertAndAllocateEnrolmentService {

  type UpsertAndAllocateEnrolmentResponse = Either[UpsertAndAllocateEnrolmentFailure, UpsertAndAllocateEnrolmentSuccess.type]

  case object UpsertAndAllocateEnrolmentSuccess

  sealed trait UpsertAndAllocateEnrolmentFailure

  case object UpsertKnownFactsFailure extends UpsertAndAllocateEnrolmentFailure

  case object NoGroupIdFailure extends UpsertAndAllocateEnrolmentFailure

  case object NoCredentialsFailure extends UpsertAndAllocateEnrolmentFailure

  case object AllocateEnrolmentFailure extends UpsertAndAllocateEnrolmentFailure

}

