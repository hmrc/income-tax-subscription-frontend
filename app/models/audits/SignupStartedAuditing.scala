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

object SignupStartedAuditing {

  val signupStartedAuditType = "mtdITSASignUpStarted"

  case class SignupStartedAuditModel(agentReferenceNumber: Option[String], utr: Option[String], nino: Option[String]) extends AuditModel {
    override val transactionName: Option[String] = None
    val userType = if (agentReferenceNumber.isDefined) "agent" else "individual"
    val ninoKeyValue = nino.map("nino" -> _)
    val utrKeyValue = utr.map("saUtr" -> _)
    val arnKeyValue = agentReferenceNumber.map("agentReferenceNumber" -> _)
    override val detail: Map[String, String] = Map(
      "userType" -> userType
    ) ++ arnKeyValue ++ ninoKeyValue ++ utrKeyValue

    override val auditType: String = signupStartedAuditType
  }
}
