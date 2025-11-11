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
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsBoolean, JsString, Json}
import services.IndividualStartOfJourneyThrottle

class SessionDataSpec extends PlaySpec {

  private val reference = "A/1234-B"
  private val throttle = IndividualStartOfJourneyThrottle
  private val msndationStatus = MandationStatusModel(Voluntary, Voluntary)
  private val eligibilityStatus = EligibilityStatus(false, false, None)
  private val nino = "AB012345C"
  private val utr = "0123456789"
  private val softwareStatus = Yes
  private val consentStatus = Yes
  private val emailPassed = true

  private val sessionData = SessionData(Map(
    ITSASessionKeys.REFERENCE -> JsString(reference),
    ITSASessionKeys.throttlePassed(throttle) -> JsBoolean(true),
    ITSASessionKeys.MANDATION_STATUS -> Json.toJson(msndationStatus),
    ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(eligibilityStatus),
    ITSASessionKeys.NINO -> JsString(nino),
    ITSASessionKeys.UTR -> JsString(utr),
    ITSASessionKeys.HAS_SOFTWARE -> JsString(softwareStatus.toString),
    ITSASessionKeys.CAPTURE_CONSENT -> JsString(consentStatus.toString),
    ITSASessionKeys.EMAIL_PASSED -> JsBoolean(emailPassed)
  ))

  "fetchReference" in {
    sessionData.fetchReference mustBe Some(reference)
  }

  "fetchThrottlePassed" in {
    sessionData.fetchThrottlePassed(throttle) mustBe Some(true)
  }

  "fetchMandationStatus" in {
    sessionData.fetchMandationStatus mustBe Some(msndationStatus)
  }

  "fetchEligibilityStatus" in {
    sessionData.fetchEligibilityStatus mustBe Some(eligibilityStatus)
  }

  "fetchNino" in {
    sessionData.fetchNino mustBe Some(nino)
  }

  "fetchUTR" in {
    sessionData.fetchUTR mustBe Some(utr)
  }

  "fetchSoftwareStatus" in {
    sessionData.fetchSoftwareStatus mustBe Some(softwareStatus)
  }

  "fetchConsentStatus" in {
    sessionData.fetchConsentStatus mustBe Some(consentStatus)
  }

  "fetchEmailPassed" in {
    sessionData.fetchEmailPassed mustBe Some(emailPassed)
  }
}
