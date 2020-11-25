/*
 * Copyright 2020 HM Revenue & Customs
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

object ClientMatchingAuditing {

  val clientMatchingTransactionName = "ITSAMatchingRequest"
  val clientMatchingAuditType = "ClientMatchingCheckSubmitted"

  case class ClientMatchingAuditModel(agentReferenceNumber: String, userDetailsModel: UserDetailsModel, isSuccess: Boolean) extends AuditModel {
    override val transactionName: Option[String] = Some(clientMatchingTransactionName)
    override val detail: Map[String, String] = Map(
      "agentReferenceNumber" -> agentReferenceNumber,
      "firstName" -> userDetailsModel.firstName,
      "lastName" -> userDetailsModel.lastName,
      "nino" -> userDetailsModel.ninoInBackendFormat,
      "dateOfBirth" -> userDetailsModel.dateOfBirth.toDesDateFormat,
      "matchSuccess" -> s"$isSuccess"
    )
    override val auditType: String = clientMatchingAuditType
  }

}
