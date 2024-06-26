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
import models.EligibilityStatus
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

  def fetchThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[Boolean]] = {
    sessionDataConnector.getSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle))
  }

  def saveThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData[Boolean](ITSASessionKeys.throttlePassed(throttle), true)
  }

  def deleteThrottlePassed(throttle: Throttle)(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    sessionDataConnector.deleteSessionData(ITSASessionKeys.throttlePassed(throttle))
  }

  def fetchMandationStatus(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[MandationStatusModel]] = {
    sessionDataConnector.getSessionData[MandationStatusModel](ITSASessionKeys.MANDATION_STATUS)
  }

  def saveMandationStatus(mandationStatus: MandationStatusModel)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.MANDATION_STATUS, mandationStatus)
  }

  def deleteMandationStatus(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    sessionDataConnector.deleteSessionData(ITSASessionKeys.MANDATION_STATUS)
  }

  def fetchEligibilityStatus(implicit hc: HeaderCarrier): Future[GetSessionDataResponse[EligibilityStatus]] = {
    sessionDataConnector.getSessionData[EligibilityStatus](ITSASessionKeys.ELIGIBILITY_STATUS)
  }

  def saveEligibilityStatus(eligibilityStatus: EligibilityStatus)(implicit hc: HeaderCarrier): Future[SaveSessionDataResponse] = {
    sessionDataConnector.saveSessionData(ITSASessionKeys.ELIGIBILITY_STATUS, eligibilityStatus)
  }

  def deleteEligibilityStatus(implicit hc: HeaderCarrier): Future[DeleteSessionDataResponse] = {
    sessionDataConnector.deleteSessionData(ITSASessionKeys.ELIGIBILITY_STATUS)
  }

}