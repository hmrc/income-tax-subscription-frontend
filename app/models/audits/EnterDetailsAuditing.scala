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

package models.audits

import models.usermatching.UserDetailsModel
import services.AuditModel

object EnterDetailsAuditing {

  val enterDetailsAuditType: String = "EnterDetails"

  case class EnterDetailsAuditModel(agentReferenceNumber: String,
                                    userDetails: UserDetailsModel,
                                    numberOfAttempts: Int,
                                    lockedOut: Boolean) extends AuditModel {

    override val transactionName: Option[String] = None

    override val detail: Map[String, String] = Map(
      "agentReferenceNumber" -> agentReferenceNumber,
      "userType" -> "agent",
      "firstName" -> userDetails.firstName,
      "lastName" -> userDetails.lastName,
      "dateOfBirth" -> userDetails.dateOfBirth.toDesDateFormat,
      "nino" -> userDetails.nino,
      "numberOfAttempts" -> numberOfAttempts.toString,
      "lockedOut" -> lockedOut.toString
    )

    override val auditType: String = enterDetailsAuditType

  }

}
