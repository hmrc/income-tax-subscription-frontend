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

import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
import services.IndividualStartOfJourneyThrottle
import _root_.common.Constants.ITSASessionKeys

class SessionDataSpec extends PlaySpec {
  
  private val sessionData = SessionData()

  "Reference" in {
    val reference = "A/1234-B"
    sessionData.fetchReference mustBe None
    sessionData.saveReference(reference)
    sessionData.fetchReference mustBe Some(reference)
    sessionData.deleteReference()
    sessionData.fetchReference mustBe None
  }

  "ThrottlePassed" in {
    val throttle = IndividualStartOfJourneyThrottle
    sessionData.fetchThrottlePassed(throttle) mustBe None
    sessionData.saveThrottlePassed(throttle)
    sessionData.fetchThrottlePassed(throttle) mustBe Some(true)
    sessionData.deleteThrottlePassed(throttle)
    sessionData.fetchThrottlePassed(throttle) mustBe None
  }

  "MandationStatus" in {
    val msndationStatus = MandationStatusModel(Voluntary, Voluntary)
    sessionData.fetchMandationStatus mustBe None
    sessionData.saveMandationStatus(msndationStatus)
    sessionData.fetchMandationStatus mustBe Some(msndationStatus)
    sessionData.deleteMandationStatus()
    sessionData.fetchMandationStatus mustBe None
  }

  "EligibilityStatus" in {
    val eligibilityStatus = EligibilityStatus(false, false)
    sessionData.fetchEligibilityStatus mustBe None
    sessionData.saveEligibilityStatus(eligibilityStatus)
    sessionData.fetchEligibilityStatus mustBe Some(eligibilityStatus)
    sessionData.deleteEligibilityStatus()
    sessionData.fetchEligibilityStatus mustBe None
  }

  "Nino" in {
    val nino = "AB012345C"
    sessionData.fetchNino mustBe None
    sessionData.saveNino(nino)
    sessionData.fetchNino mustBe Some(nino)
    sessionData.deleteNino()
    sessionData.fetchNino mustBe None
  }

  "UTR" in {
    val utr = "0123456789"
    sessionData.fetchUTR mustBe None
    sessionData.saveUTR(utr)
    sessionData.fetchUTR mustBe Some(utr)
    sessionData.deleteUTR()
    sessionData.fetchUTR mustBe None
  }

  "SoftwareStatus" in {
    val softwareStatus = Yes
    sessionData.fetchSoftwareStatus mustBe None
    sessionData.saveSoftwareStatus(softwareStatus)
    sessionData.fetchSoftwareStatus mustBe Some(softwareStatus)
    sessionData.deleteSoftwareStatus()
    sessionData.fetchSoftwareStatus mustBe None
  }

  "ConsentStatus" in {
    val consentStatus = Yes
    sessionData.fetchConsentStatus mustBe None
    sessionData.saveConsentStatus(consentStatus)
    sessionData.fetchConsentStatus mustBe Some(consentStatus)
    sessionData.deleteConsentStatus()
    sessionData.fetchConsentStatus mustBe None
  }

  "EmailPassed" in {
    val emailPassed = true
    sessionData.fetchEmailPassed mustBe None
    sessionData.saveEmailPassed(emailPassed)
    sessionData.fetchEmailPassed mustBe Some(emailPassed)
    sessionData.deleteEmailPassed()
    sessionData.fetchEmailPassed mustBe None
  }

  "clear" in {
    val sessionData = SessionData(Map(
      "A" -> JsString("A")
    ))
    sessionData.isEmpty mustBe false
    sessionData.clear()
    sessionData.isEmpty mustBe true
  }

  "diff" in {
    val sessionData = SessionData(Map(
      ITSASessionKeys.REFERENCE -> JsString("A")
    ))
    sessionData.saveReference("B")
    sessionData.saveNino("C")
    sessionData.diff mustBe Map(
      ITSASessionKeys.REFERENCE -> JsString("B"),
      ITSASessionKeys.NINO -> JsString("C")
    )
  }
}
