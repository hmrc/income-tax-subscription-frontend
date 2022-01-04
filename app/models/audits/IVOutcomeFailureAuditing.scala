/*
 * Copyright 2022 HM Revenue & Customs
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

object IVOutcomeFailureAuditing {

  val ivOutcomeAuditType: String = "LowConfidenceLevelIvOutcomeFailure"

  case class IVOutcomeFailureAuditModel(ivJourneyId: String) extends AuditModel {

    override val transactionName: Option[String] = None

    override val detail: Map[String, String] = Map(
      "ivJourneyId" -> ivJourneyId
    )

    override val auditType: String = ivOutcomeAuditType

  }

}

