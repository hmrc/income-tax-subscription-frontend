/*
 * Copyright 2026 HM Revenue & Customs
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

import models.EligibilityStatus
import models.common.business.Address
import models.status.{MandationStatus, MandationStatusModel}
import play.api.libs.json.*
import play.api.libs.json.Format.GenericFormat
import services.GetCompleteDetailsService.CompleteDetails
import services.JsonAuditModel
import utilities.AccountingPeriodUtil

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ITSASignUpSubmissionRequestAuditing {

  private val ITSASignUpSubmissionRequestAudit: String = "ITSASignUpSubmissionRequest"

  case class ITSASignUpSubmissionRequestAuditModel(agentReferenceNumber: Option[String],
                                                   utr: String,
                                                   nino: String,
                                                   eligibility: EligibilityStatus,
                                                   itsaStatus: MandationStatusModel,
                                                   completeDetails: CompleteDetails)
    extends JsonAuditModel {

    private def localDateStringFormat(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private def startDateJsonBlock(startDate: Option[LocalDate]): JsObject = Json.obj(
      "startDateLimit" -> localDateStringFormat(AccountingPeriodUtil.getStartDateLimit),
      "startDateBeforeLimit" -> startDate.fold("yes")(_ => "no")
    ) ++ startDate.fold(Json.obj())(date => Json.obj("startDate" -> localDateStringFormat(date)))

    private def mandationStatusToLabel(mandationStatus: MandationStatus) = {
      mandationStatus match {
        case MandationStatus.Mandated => "Mandated"
        case MandationStatus.Voluntary => "Voluntary"
      }
    }

    // helper to reduce repetition when adding optional fields to JsObjects
    // Accepts any A with an implicit Writes[A] so it can be used for Strings, JsValue, or any other type
    private def optField[A](key: String, opt: Option[A])(implicit writes: Writes[A]): JsObject =
      opt.fold(Json.obj())(v => Json.obj(key -> v))

    private def addressJson(address: Address): JsObject = Json.obj(
      "lines" -> address.lines
    ) ++
      optField("uprn", address.uprn) ++
      optField("postcode", address.postcode) ++
      address.country.fold(Json.obj()) { country =>
        Json.obj(
          "country" -> Json.obj(
            "code" -> country.code,
            "name" -> country.name
          )
        )
      }

    private val selfEmploymentJson: Option[JsValue] = {
      completeDetails.incomeSources.soleTraderBusinesses.map { soleTraderBusinesses =>
        Json.arr(
          soleTraderBusinesses.businesses.map { business =>
            startDateJsonBlock(business.startDate) ++ Json.obj(
              "trade" -> business.trade,
              "name" -> business.name,
              "address" -> addressJson(business.address)
            )
          }: _*
        )
      }
    }

    private val ukPropertyJson: Option[JsValue] = {
      completeDetails.incomeSources.ukProperty.map { ukProperty =>
        startDateJsonBlock(ukProperty.startDate)
      }
    }

    private val foreignPropertyJson: Option[JsValue] = {
      completeDetails.incomeSources.foreignProperty.map { foreignProperty =>
        startDateJsonBlock(foreignProperty.startDate)
      }
    }

    private val incomeSourcesJson: JsValue = Json.obj() ++
      optField("selfEmployment", selfEmploymentJson) ++
      optField("ukProperty", ukPropertyJson) ++
      optField("foreignProperty", foreignPropertyJson)

    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"

    override val auditType: String = ITSASignUpSubmissionRequestAudit
    override val detail: JsValue = Json.obj(
      "userType" -> userType,
      "nino" -> nino,
      "utr" -> utr,
      "taxYear" -> completeDetails.taxYear.toFullYearFormat,
      "signUpTaxYears" -> Json.obj(
        "currentYear" -> AccountingPeriodUtil.getCurrentTaxYear.toFullTaxYear,
        "nextYear" -> AccountingPeriodUtil.getNextTaxYear.toFullTaxYear
      ),
      "itsaStatus" -> Json.obj(
        "currentYearStatus" -> mandationStatusToLabel(itsaStatus.currentYearStatus),
        "nextYearStatus" -> mandationStatusToLabel(itsaStatus.nextYearStatus)
      ),
      "eligibilityStatus" -> Json.obj(
        "currentYearStatus" -> (if (eligibility.eligibleCurrentYear) "Eligible" else "Ineligible"),
        "nextYearStatus" -> (if (eligibility.eligibleNextYear) "Eligible" else "Ineligible")
      ),
      "incomeSources" -> incomeSourcesJson
    ) ++ optField("agentReferenceNumber", agentReferenceNumber)

  }
}
