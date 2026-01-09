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
import models.status.MandationStatusModel
import models.{EligibilityStatus, SessionData, YesNo}
import play.api.libs.json.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def getAllSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SessionData] = {
    sessionDataConnector.getAllSessionData().map {
      case Right(value) => SessionData(value.getOrElse(Map()))
      case _ => SessionData()
    }
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

  def saveThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle), true)
  }

  def saveMandationStatus(mandationStatus: MandationStatusModel)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.MANDATION_STATUS, Json.toJson(mandationStatus))
  }
  
  def saveSignedUpDate(date: LocalDate)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.SIGNED_UP_DATE, Json.toJson(date))
  }

  def saveEligibilityStatus(eligibilityStatus: EligibilityStatus)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, Json.toJson(eligibilityStatus))
  }

  def saveNino(nino: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.NINO, nino)
  }

  def saveUTR(utr: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.UTR, utr)
  }

  def saveSoftwareStatus(softwareStatus: YesNo)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.HAS_SOFTWARE, Json.toJson(softwareStatus))
  }

  def saveConsentStatus(consentStatus: YesNo)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.CAPTURE_CONSENT, Json.toJson(consentStatus))
  }

  def saveEmailPassed(emailPassed: Boolean)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.EMAIL_PASSED, JsBoolean(emailPassed))
  }
}
