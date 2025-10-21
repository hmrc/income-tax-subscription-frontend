/*
 * Copyright 2024 HM Revenue & Customs
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

import common.Constants.ITSASessionKeys
import connectors.SessionDataConnector
import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataResponse
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataResponse
import models.SessionData.Data
import models.status.MandationStatusModel
import models.{EligibilityStatus, YesNo}
import org.parboiled2.RuleTrace.StringMatch
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  implicit class JsObject(value: JsValue) {
    def toObject[T](implicit reads: Reads[T]): T = {
      Json.fromJson[T](value) match {
        case JsSuccess(value, _) => value
        case JsError(e) => throw new Exception(s"Invalid Json: $e")
      }
    }
  }

  def getAllSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Data] = {
    sessionDataConnector.getAllSessionData().map {
      case Right(value) => value.getOrElse(Map())
      case _ => Map()
    }
  }

  def fetchReference(sessionData: Data): Option[String] = {
    sessionData.get(ITSASessionKeys.REFERENCE).map(_.toObject[String])
  }

  def saveReference(reference: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData[String](ITSASessionKeys.REFERENCE, reference)
  }

  def deleteReference(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    sessionDataConnector.deleteSessionData(ITSASessionKeys.REFERENCE)
  }

  def deleteSessionAll(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    sessionDataConnector.deleteAllSessionData
  }

  def fetchThrottlePassed(sessionData: Data, throttle: Throttle): Option[Boolean] = {
    sessionData.get(ITSASessionKeys.throttlePassed(throttle)).map(_.toObject[Boolean])
  }

  def saveThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle), true)
  }

  def fetchMandationStatus(sessionData: Data): Option[MandationStatusModel] = {
    sessionData.get(ITSASessionKeys.MANDATION_STATUS).map(_.toObject[MandationStatusModel])
  }

  def saveMandationStatus(mandationStatus: MandationStatusModel)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.MANDATION_STATUS, Json.toJson(mandationStatus))
  }

  def fetchEligibilityStatus(sessionData: Data): Option[EligibilityStatus] = {
    sessionData.get(ITSASessionKeys.ELIGIBILITY_STATUS).map(_.toObject[EligibilityStatus])
  }

  def saveEligibilityStatus(eligibilityStatus: EligibilityStatus)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, Json.toJson(eligibilityStatus))
  }

  def fetchNino(sessionData: Data): Option[String] = {
    sessionData.get(ITSASessionKeys.NINO).map(_.toObject[String])
  }

  def saveNino(nino: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.NINO, nino)
  }

  def fetchUTR(sessionData: Data): Option[String] = {
    sessionData.get(ITSASessionKeys.UTR).map(_.toObject[String])
  }

  def saveUTR(utr: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.UTR, utr)
  }

  def fetchSoftwareStatus(sessionData: Data): Option[YesNo] = {
    sessionData.get(ITSASessionKeys.HAS_SOFTWARE).map(_.toObject[YesNo])
  }

  def saveSoftwareStatus(softwareStatus: YesNo)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.HAS_SOFTWARE, Json.toJson(softwareStatus))
  }

  def fetchConsentStatus(sessionData: Data): Option[YesNo] = {
    sessionData.get(ITSASessionKeys.CAPTURE_CONSENT).map(_.toObject[YesNo])
  }

  def saveConsentStatus(consentStatus: YesNo)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.CAPTURE_CONSENT, Json.toJson(consentStatus))
  }

  def fetchEmailPassed(sessionData: Data): Option[Boolean] = {
    sessionData.get(ITSASessionKeys.EMAIL_PASSED).map(_.toObject[Boolean])
  }

  def saveEmailPassed(emailPassed: Boolean)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.EMAIL_PASSED, emailPassed.toString)
  }
}
