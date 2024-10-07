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
import connectors.httpparser.GetSessionDataHttpParser.GetSessionDataResponse
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataResponse
import models.{EligibilityStatus, YesNo}
import models.status.MandationStatusModel
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SessionDataService @Inject()(sessionDataConnector: SessionDataConnector) {

  def fetchReference(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[String]] = {
    sessionDataConnector.getSessionData[String](ITSASessionKeys.REFERENCE)
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

  def fetchThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[Boolean]] = {
    sessionDataConnector.getSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle))
  }

  def saveThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle), true)
  }

  def fetchMandationStatus(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[MandationStatusModel]] = {
    sessionDataConnector.getSessionData[MandationStatusModel](ITSASessionKeys.MANDATION_STATUS)
  }

  def saveMandationStatus(mandationStatus: MandationStatusModel)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.MANDATION_STATUS, mandationStatus)
  }

  def fetchEligibilityStatus(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[EligibilityStatus]] = {
    sessionDataConnector.getSessionData[EligibilityStatus](ITSASessionKeys.ELIGIBILITY_STATUS)
  }

  def saveEligibilityStatus(eligibilityStatus: EligibilityStatus)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, eligibilityStatus)
  }

  def fetchNino(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[String]] = {
    sessionDataConnector.getSessionData[String](ITSASessionKeys.NINO)
  }

  def saveNino(nino: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.NINO, nino)
  }

  def fetchUTR(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[String]] = {
    sessionDataConnector.getSessionData[String](ITSASessionKeys.UTR)
  }

  def saveUTR(utr: String)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.UTR, utr)
  }

  def fetchSoftwareStatus(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[YesNo]] = {
    sessionDataConnector.getSessionData[YesNo](ITSASessionKeys.HAS_SOFTWARE)
  }

  def saveSoftwareStatus(softwareStatus: YesNo)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.HAS_SOFTWARE, softwareStatus)
  }

}