/*
 * Copyright 2025 HM Revenue & Customs
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

case class BetaContactDetails(emailAddress: String, agentReferenceNumber: Option[String], fullName: Option[String], nino: Option[String]) extends AuditModel {

  val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
  private val agentReferenceNumberKeyValue: Option[(String, String)] = agentReferenceNumber.map("agentReferenceNumber" -> _)
  private val fullNameKeyValue: Option[(String, String)] = fullName.map("fullName" -> _)
  private val ninoKeyValue: Option[(String, String)] = nino.map("nino" -> _)

  override val auditType: String = "BetaContactDetails"
  override val detail: Map[String, String] = Map(
    "userType" -> userType,
    "emailAddress" -> emailAddress
  ) ++ fullNameKeyValue ++ agentReferenceNumberKeyValue ++ ninoKeyValue

  override val transactionName: Option[String] = None
}
