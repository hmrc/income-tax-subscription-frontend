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

package models

import models.status.MandationStatusModel
import play.api.libs.json.{JsBoolean, JsError, JsString, JsSuccess, JsValue, Json, Reads}
import services.Throttle
import _root_.common.Constants.ITSASessionKeys

case class SessionData(
  initial: Map[String, JsValue] = Map()
) {
  implicit class JsObject(value: JsValue) {
    def toObject[T](implicit reads: Reads[T]): T = {
      Json.fromJson[T](value) match {
        case JsSuccess(value, _) => value
        case JsError(e) => throw new Exception(s"Invalid Json: $e")
      }
    }
  }

  private val current =
    collection.mutable.Map(initial.toSeq: _*)

  private def deleteKey(key: String): Unit = {
    current.filterInPlace(
      (k, _) => k != key
    )
  }

  def fetchReference: Option[String] = {
    current.get(ITSASessionKeys.REFERENCE).map(_.toObject[String])
  }

  def fetchThrottlePassed(throttle: Throttle): Option[Boolean] = {
    current.get(ITSASessionKeys.throttlePassed(throttle)).map(_.toObject[Boolean])
  }

  def fetchMandationStatus: Option[MandationStatusModel] = {
    current.get(ITSASessionKeys.MANDATION_STATUS).map(_.toObject[MandationStatusModel])
  }

  def fetchEligibilityStatus: Option[EligibilityStatus] = {
    current.get(ITSASessionKeys.ELIGIBILITY_STATUS).map(_.toObject[EligibilityStatus])
  }

  def fetchNino: Option[String] = {
    current.get(ITSASessionKeys.NINO).map(_.toObject[String])
  }

  def fetchUTR: Option[String] = {
    current.get(ITSASessionKeys.UTR).map(_.toObject[String])
  }

  def fetchSoftwareStatus: Option[YesNo] = {
    current.get(ITSASessionKeys.HAS_SOFTWARE).map(_.toObject[YesNo])
  }

  def fetchConsentStatus: Option[YesNo] = {
    current.get(ITSASessionKeys.CAPTURE_CONSENT).map(_.toObject[YesNo])
  }

  def fetchEmailPassed: Option[Boolean] = {
    current.get(ITSASessionKeys.EMAIL_PASSED).map(_.toObject[Boolean])
  }

  def saveReference(reference: String): Unit = {
    current.put(ITSASessionKeys.REFERENCE, JsString(reference))
  }

  def saveThrottlePassed(throttle: Throttle): Unit = {
    current.put(ITSASessionKeys.throttlePassed(throttle), JsBoolean(true))
  }

  def saveMandationStatus(mandationStatus: MandationStatusModel): Unit = {
    current.put(ITSASessionKeys.MANDATION_STATUS, Json.toJson(mandationStatus))
  }

  def saveEligibilityStatus(eligibilityStatus: EligibilityStatus): Unit = {
    current.put(ITSASessionKeys.ELIGIBILITY_STATUS, Json.toJson(eligibilityStatus))
  }

  def saveNino(nino: String): Unit = {
    current.put(ITSASessionKeys.NINO, JsString(nino))
  }

  def saveUTR(utr: String): Unit = {
    current.put(ITSASessionKeys.UTR, JsString(utr))
  }

  def saveSoftwareStatus(softwareStatus: YesNo): Unit = {
    current.put(ITSASessionKeys.HAS_SOFTWARE, Json.toJson(softwareStatus))
  }

  def saveConsentStatus(consentStatus: YesNo): Unit = {
    current.put(ITSASessionKeys.CAPTURE_CONSENT, Json.toJson(consentStatus))
  }

  def saveEmailPassed(emailPassed: Boolean): Unit = {
    current.put(ITSASessionKeys.EMAIL_PASSED, JsBoolean(emailPassed))
  }

  def deleteReference(): Unit = {
    deleteKey(ITSASessionKeys.REFERENCE)
  }

  def deleteThrottlePassed(throttle: Throttle): Unit = {
    deleteKey(ITSASessionKeys.throttlePassed(throttle))
  }

  def deleteMandationStatus(): Unit = {
    deleteKey(ITSASessionKeys.MANDATION_STATUS)
  }

  def deleteEligibilityStatus(): Unit = {
    deleteKey(ITSASessionKeys.ELIGIBILITY_STATUS)
  }

  def deleteNino(): Unit = {
    deleteKey(ITSASessionKeys.NINO)
  }

  def deleteUTR(): Unit = {
    deleteKey(ITSASessionKeys.UTR)
  }

  def deleteSoftwareStatus(): Unit = {
    deleteKey(ITSASessionKeys.HAS_SOFTWARE)
  }

  def deleteConsentStatus(): Unit = {
    deleteKey(ITSASessionKeys.CAPTURE_CONSENT)
  }

  def deleteEmailPassed(): Unit = {
    deleteKey(ITSASessionKeys.EMAIL_PASSED)
  }

  def clear(): Unit = {
    current.clear()
  }

  def isEmpty: Boolean =
    current.isEmpty

  def diff(): Map[String, JsValue] =
    Map(current.filter(
      e => !initial.get(e._1).contains(e._2)
    ).toSeq: _*)
}
