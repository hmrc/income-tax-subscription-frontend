/*
 * Copyright 2023 HM Revenue & Customs
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

package common

object Constants {
  val mtdItsaEnrolmentName = "HMRC-MTD-IT"
  val hmrcAsAgent = "HMRC-AS-AGENT"
  val mtdItsaEnrolmentIdentifierKey = "MTDITID"

  val agentServiceIdentifierKey = "AgentReferenceNumber"

  val ninoEnrolmentName = "HMRC-NI"
  val ninoEnrolmentIdentifierKey = "NINO"
  val utrEnrolmentName = "IR-SA"
  val utrEnrolmentIdentifierKey = "UTR"

  object GovernmentGateway {
    val GGProviderId = "GovernmentGateway"
    val MTDITID = "MTDITID"
    val NINO = "NINO"
    val ggPortalId = "Default"
    val ggServiceName = "HMRC-MTD-IT"
    val ggFriendlyName = "Making Tax Digital Income Tax Self-Assessment enrolment"
  }

  object ITSASessionKeys {

    val ArnKey = "ARN"
    val MTDITID: String = mtdItsaEnrolmentIdentifierKey
    val RequestURI = "Request-URI"
    val FailedClientMatching = "Failed-Client-Matching"
    val JourneyStateKey = "Journey-State"
    val NINO = "NINO"
    val UTR = "UTR"
    val FULLNAME: String = "FULLNAME"
    val REFERENCE = "reference"
    val ELIGIBLE_NEXT_YEAR_ONLY = "ELIGIBLE_NEXT_YEAR_ONLY"

    val sessionId = "sessionId"

    val clientData: Seq[String] = Seq(MTDITID, NINO, UTR)

    val StartTime = "StartTime"
    val ConfirmedAgent = "Confirmed-Agent"
    val IdentityVerificationFlag = "ITSA-Identity-Verification-Flag"

    val SPSEntityId: String = "SPS-Entity-ID"

  }
}
