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

import connectors.httpparser.{RetrieveReferenceHttpParser, SaveSessionDataHttpParser}
import models.SessionData.Data
import models.audits.SignupRetrieveAuditing.SignupRetrieveAuditModel
import play.api.mvc.Request
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

  def getIndividualReference(sessionData: Data = Map())(implicit hc: HeaderCarrier, request: Request[_]): Future[String] = {
    getReference(arn = None, sessionData)
  }

  def getAgentReference(sessionData: Data = Map())(implicit hc: HeaderCarrier, request: Request[_], userArn: String): Future[String] = {
    getReference(arn = Some(userArn), sessionData)
  }

  def getReference(arn: Option[String], sessionData: Data)
                  (implicit hc: HeaderCarrier, request: Request[_]): Future[String] = {

    sessionDataService.fetchReference(sessionData) match {
      case Some(reference) => Future.successful(reference)
      case None =>
        ninoService.getNino(sessionData) flatMap { nino =>
          utrService.getUTR(sessionData) flatMap { utr =>
            handleReferenceNotFound(nino, utr, arn)
          }
        }
    }
  }

  private def handleReferenceNotFound(nino: String, utr: String, arn: Option[String])
                                     (implicit hc: HeaderCarrier, request: Request[_]): Future[String] = {
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
