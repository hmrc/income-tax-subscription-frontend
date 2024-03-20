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

package controllers.utils

import auth.agent.IncomeTaxAgentUser
import auth.individual.IncomeTaxSAUser
import connectors.httpparser.{GetSessionDataHttpParser, RetrieveReferenceHttpParser, SaveSessionDataHttpParser}
import models.audits.SignupRetrieveAuditing.SignupRetrieveAuditModel
import play.api.mvc.{AnyContent, Request, Result}
import services.{AuditingService, SessionDataService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

trait ReferenceRetrieval {

  val subscriptionDetailsService: SubscriptionDetailsService
  val sessionDataService: SessionDataService
  val auditingService: AuditingService
  implicit val ec: ExecutionContext

  def withIndividualReference(f: String => Future[Result])
                             (implicit hc: HeaderCarrier, request: Request[AnyContent], user: IncomeTaxSAUser): Future[Result] = {
    withReference(
      utr = user.utr.getOrElse(
        throw new InternalServerException("[ReferenceRetrieval][withIndividualReference] - Unable to retrieve users utr")
      ),
      nino = user.nino,
      arn = None
    )(f)
  }

  def withAgentReference(f: String => Future[Result])
                        (implicit hc: HeaderCarrier, request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    withReference(
      user.clientUtr.getOrElse(
        throw new InternalServerException("[ReferenceRetrieval][withAgentReference] - Unable to retrieve clients utr")
      ),
      nino = user.clientNino,
      arn = Some(user.arn)
    )(f)
  }

  def withReference(utr: String, nino: Option[String], arn: Option[String])
                   (f: String => Future[Result])
                   (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    sessionDataService.fetchReference.flatMap {
      case Right(Some(reference)) => f(reference)
      case Right(None) => handleReferenceNotFound(utr, nino, arn)(f)
      case Left(GetSessionDataHttpParser.InvalidJson) =>
        throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Unable to parse json returned from session")
      case Left(GetSessionDataHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Error occurred when fetching reference from session. Status: $status")
    }
  }

  private def handleReferenceNotFound(utr: String, nino: Option[String], arn: Option[String])
                                     (f: String => Future[Result])
                                     (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    subscriptionDetailsService.retrieveReference(utr) flatMap {
      case Right(RetrieveReferenceHttpParser.Existing(reference)) =>
        auditingService.audit(SignupRetrieveAuditModel(arn, utr, nino))
          .flatMap(_ => saveReferenceToSession(reference)(f))
      case Right(RetrieveReferenceHttpParser.Created(reference)) =>
        saveReferenceToSession(reference)(f)
      case Left(RetrieveReferenceHttpParser.InvalidJsonFailure) =>
        throw new InternalServerException(s"[ReferenceRetrieval][handleReferenceNotFound] - Unable to parse json returned")
      case Left(RetrieveReferenceHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][handleReferenceNotFound] - Unexpected status returned: $status")
    }
  }

  private def saveReferenceToSession(reference: String)
                                    (f: String => Future[Result])
                                    (implicit hc: HeaderCarrier): Future[Result] = {
    sessionDataService.saveReference(reference) flatMap {
      case Right(_) =>
        f(reference)
      case Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][saveReferenceToSession] - Unexpected status returned: $status")
    }
  }

}
