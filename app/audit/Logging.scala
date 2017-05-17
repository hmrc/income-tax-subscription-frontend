/*
 * Copyright 2017 HM Revenue & Customs
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

package audit

import javax.inject.{Inject, Singleton}

import play.api.libs.json.JsValue
import play.api.{Application, Configuration, Logger}
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.http.HeaderCarrier

case class LoggingConfig(heading: String)

object LoggingConfig {

  implicit class LoggingConfigUtil(config: Option[LoggingConfig]) {
    def addHeading(message: String): String = config.fold(message)(x => x.heading + ": " + message)
  }

}

import utils.JsonUtils._

@Singleton
class Logging @Inject()(application: Application,
                        configuration: Configuration,
                        auditConnector: AuditConnector) {


  lazy val appName: String = configuration.getString("appName").fold("APP NAME NOT SET")(x => x)

  lazy val debugToWarn: Boolean = configuration.getString("feature-switching.debugToWarn").fold(false)(x => x.toBoolean)

  lazy val audit: Audit = new Audit(appName, auditConnector)

  private def sendDataEvent(transactionName: String,
                            path: String = "N/A",
                            tags: Map[String, String] = Map.empty[String, String],
                            detail: Map[String, String],
                            auditType: String)
                           (implicit hc: HeaderCarrier): Unit = {
    val packet = DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, path) ++ tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(detail.toSeq: _*)
    )
    val pjs = packet: JsValue
    audit.sendDataEvent(packet)
  }

  private def splunkToLogger(transactionName: String, detail: Map[String, String], auditType: String)(implicit hc: HeaderCarrier): String =
    s"""| Audit Source: $appName
        | Audit Type: $auditType
        | Transaction Name: $transactionName
        | Header Carrier:
        | $hc
        | Request Details:
        | $detail
    """.stripMargin

  private def splunkFunction(transactionName: String, detail: Map[String, String], auditType: String)(implicit hc: HeaderCarrier) = {
    val loggingFunc: String => Unit = if (debugToWarn) Logger.warn(_) else Logger.debug(_)
    loggingFunc(Logging.splunkString + splunkToLogger(transactionName, detail, auditType))
    sendDataEvent(
      transactionName = transactionName,
      detail = detail,
      auditType = auditType
    )
  }

  def audit(transactionName: String, detail: Map[String, String], auditType: String)(implicit hc: HeaderCarrier): Unit =
    splunkFunction(transactionName, detail, auditType)

  def auditFor(transactionName: String)(implicit hc: HeaderCarrier): (Map[String, String], String) => Unit = audit(transactionName, _, _)(hc)

  def auditFor(transactionName: String, detail: Map[String, String])(implicit hc: HeaderCarrier): String => Unit = audit(transactionName, detail, _)(hc)

  @inline def trace(msg: String)(implicit config: Option[LoggingConfig] = None): Unit = Logger.trace(config.addHeading(msg))

  @inline def debug(msg: String)(implicit config: Option[LoggingConfig] = None): Unit = Logger.debug(config.addHeading(msg))

  @inline def info(msg: String)(implicit config: Option[LoggingConfig] = None): Unit = Logger.info(config.addHeading(msg))

  @inline def warn(msg: String)(implicit config: Option[LoggingConfig] = None): Unit = Logger.warn(config.addHeading(msg))

  @inline def err(msg: String)(implicit config: Option[LoggingConfig] = None): Unit = Logger.error(config.addHeading(msg))

}

object Logging {

  val splunkString = "SPLUNK AUDIT:\n"

  val eventTypeSurveyFeedback: String = "mtdItsaExitSurveyFeedback"

}

