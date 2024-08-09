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
import connectors.httpparser.{GetSessionDataHttpParser, RetrieveReferenceHttpParser, SaveSessionDataHttpParser}
import models.audits.SignupRetrieveAuditing.SignupRetrieveAuditModel
import play.api.mvc.{AnyContent, Request}
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReferenceRetrieval @Inject()(subscriptionDetailsService: SubscriptionDetailsService,
                                   sessionDataService: SessionDataService,
                                   ninoService: NinoService,
                                   utrService: UTRService,
                                   auditingService: AuditingService)
                                  (implicit ec: ExecutionContext) {

  def getIndividualReference(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[String] = {
    getReference(arn = None)
  }

  def getAgentReference(implicit hc: HeaderCarrier, request: Request[AnyContent], user: IncomeTaxAgentUser): Future[String] = {
    getReference(arn = Some(user.arn))
  }

  def getReference(arn: Option[String])
                  (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[String] = {

    sessionDataService.fetchReference.flatMap {
      case Right(Some(reference)) => Future.successful(reference)
      case Right(None) =>
        ninoService.getNino flatMap { nino =>
          utrService.getUTR flatMap { utr =>
            handleReferenceNotFound(nino, utr, arn)
          }
        }
      case Left(GetSessionDataHttpParser.InvalidJson) =>
        throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Unable to parse json returned from session")
      case Left(GetSessionDataHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][withReference] - Error occurred when fetching reference from session. Status: $status")
    }
  }

  private def handleReferenceNotFound(nino: String, utr: String, arn: Option[String])
                                     (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[String] = {
    subscriptionDetailsService.retrieveReference(utr) flatMap {
      case Right(RetrieveReferenceHttpParser.Existing(reference)) =>
        for {
          _ <- auditingService.audit(SignupRetrieveAuditModel(arn, utr, Some(nino)))
          result <- saveReferenceToSession(reference)
        } yield {
          result
        }
      case Right(RetrieveReferenceHttpParser.Created(reference)) =>
        saveReferenceToSession(reference)
      case Left(RetrieveReferenceHttpParser.InvalidJsonFailure) =>
        throw new InternalServerException(s"[ReferenceRetrieval][handleReferenceNotFound] - Unable to parse json returned")
      case Left(RetrieveReferenceHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][handleReferenceNotFound] - Unexpected status returned: $status")
    }
  }

  private def saveReferenceToSession(reference: String)
                                    (implicit hc: HeaderCarrier): Future[String] = {
    sessionDataService.saveReference(reference) map {
      case Right(_) =>
        reference
      case Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(status)) =>
        throw new InternalServerException(s"[ReferenceRetrieval][saveReferenceToSession] - Unexpected status returned: $status")
    }
  }

}
