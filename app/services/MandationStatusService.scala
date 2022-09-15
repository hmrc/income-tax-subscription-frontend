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

package services

import connectors.MandationStatusConnector
import connectors.httpparser.PostMandationStatusParser.PostMandationStatusResponse
import models.audits.MandationStatusAuditing.MandationStatusAuditModel
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandationStatusService @Inject()(val mandationStatusConnector: MandationStatusConnector,
                                       val subscriptionDetailsService: SubscriptionDetailsService,
                                       val auditingService: AuditingService) {

  def retrieveMandationStatus(reference: String, userType: String, nino: String, utr: String, arn: Option[String] = None)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext, request:Request[AnyContent]): Future[Unit] = {
    for {
      mandationStatus <- mandationStatusConnector.getMandationStatus(nino, utr)
      _ = saveMandationStatus(reference, mandationStatus, userType, nino, utr, arn)
    } yield ()
  }

  private def saveMandationStatus(
                                   reference: String,
                                   mandationStatusResponse:
                                   PostMandationStatusResponse,
                                   userType: String,
                                   nino: String,
                                   utr: String,
                                   arn: Option[String] = None
                                 )(implicit hc: HeaderCarrier, ec: ExecutionContext, request:Request[AnyContent]): Future[Unit] = {
    mandationStatusResponse match {
      case Right(mandationStatus) => {
        auditingService.audit(MandationStatusAuditModel(
          userType = userType,
          agentReferenceNumber = arn,
          utr = utr,
          nino = nino,
          currentYear = AccountingPeriodUtil.getCurrentTaxYear.toShortTaxYear,
          currentYearStatus = mandationStatus.currentYearStatus.value,
          nextYear = AccountingPeriodUtil.getNextTaxYear.toShortTaxYear,
          nextYearStatus = mandationStatus.nextYearStatus.value
        ))
        subscriptionDetailsService.saveMandationStatus(reference, mandationStatus).map(_ => ())
      }
      case Left(_) => Future.successful(())
    }
  }
}
