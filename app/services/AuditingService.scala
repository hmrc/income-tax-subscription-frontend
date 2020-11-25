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

package services

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import services.AuditingService.toDataEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext

@Singleton
class AuditingService @Inject()(appConfig: AppConfig,
                                auditConnector: AuditConnector)
                               (implicit ec: ExecutionContext) {

  def audit(auditModel: AuditModel)(implicit hc: HeaderCarrier, request: Request[_]): Unit = {
    auditConnector.sendEvent(toDataEvent(appConfig.appName, auditModel, request.path))
  }

}

object AuditingService {

  def toDataEvent(appName: String, auditModel: AuditModel, path: String)(implicit hc: HeaderCarrier): DataEvent = {
    val auditType: String = auditModel.auditType
    val transactionName: Option[String] = auditModel.transactionName
    val detail: Map[String, String] = auditModel.detail
    val tags: Map[String, String] = Map.empty[String, String]

    DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = transactionName match {
        case Some(transaction) => AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transaction, path) ++ tags
        case None => AuditExtensions.auditHeaderCarrier(hc).toAuditTags(path) ++ tags
      },
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(detail.toSeq: _*)
    )
  }

}

trait AuditModel {
  val transactionName: Option[String]
  val detail: Map[String, String]
  val auditType: String
}

