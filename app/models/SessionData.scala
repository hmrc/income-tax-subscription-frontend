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

import _root_.common.Constants.ITSASessionKeys
import models.status.MandationStatusModel
import play.api.libs.json._
import services.Throttle

case class SessionData(data: Map[String, JsValue] = Map()) {
  
  implicit class JsObject(value: JsValue) {
    def toObject[T](implicit reads: Reads[T]): T = {
      Json.fromJson[T](value) match {
        case JsSuccess(value, _) => value
        case JsError(e) => throw new Exception(s"Invalid Json: $e")
      }
    }
  }

  def fetchReference: Option[String] = {
    data.get(ITSASessionKeys.REFERENCE).map(_.toObject[String])
  }

  def fetchThrottlePassed(throttle: Throttle): Option[Boolean] = {
    data.get(ITSASessionKeys.throttlePassed(throttle)).map(_.toObject[Boolean])
  }

  def fetchMandationStatus: Option[MandationStatusModel] = {
    data.get(ITSASessionKeys.MANDATION_STATUS).map(_.toObject[MandationStatusModel])
  }

  def fetchEligibilityStatus: Option[EligibilityStatus] = {
    data.get(ITSASessionKeys.ELIGIBILITY_STATUS).map(_.toObject[EligibilityStatus])
  }

  def fetchNino: Option[String] = {
    data.get(ITSASessionKeys.NINO).map(_.toObject[String])
  }

  def fetchUTR: Option[String] = {
    data.get(ITSASessionKeys.UTR).map(_.toObject[String])
  }

  def fetchSoftwareStatus: Option[YesNo] = {
    data.get(ITSASessionKeys.HAS_SOFTWARE).map(_.toObject[YesNo])
  }

  def fetchConsentStatus: Option[YesNo] = {
    data.get(ITSASessionKeys.CAPTURE_CONSENT).map(_.toObject[YesNo])
  }

  def fetchEmailPassed: Option[Boolean] = {
    data.get(ITSASessionKeys.EMAIL_PASSED).map(_.toObject[Boolean])
  }

}
