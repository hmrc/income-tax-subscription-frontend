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

package models.audits

import services.AuditModel

object EligibilityAuditing {

  val mtdITSAEligibilityAuditType = "mtdITSAEligibility"

  case class EligibilityAuditModel(agentReferenceNumber: Option[String], utr: Option[String], nino: Option[String], eligibility: String, failureReason: Option[String]) extends AuditModel {
    override val transactionName: Option[String] = None
    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
    val ninoKeyValue: Option[(String, String)] = nino.map("nino" -> _)
    val utrKeyValue: Option[(String, String)] = utr.map("saUtr" -> _)
    val arnKeyValue: Option[(String, String)] = agentReferenceNumber.map("agentReferenceNumber" -> _)
    val eligibilityStatus: String = eligibility
    val eligibilityFailureReason: Option[(String, String)] = failureReason.map("eligibilityFailureReason" -> _)
    override val detail: Map[String, String] = Map(
      "userType" -> userType,
      "eligibilityStatus" -> eligibilityStatus
    ) ++ arnKeyValue ++ ninoKeyValue ++ utrKeyValue ++ eligibilityFailureReason

    override val auditType: String = mtdITSAEligibilityAuditType
  }
}
