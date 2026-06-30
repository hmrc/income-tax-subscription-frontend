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
import models.common.business.SelfEmploymentData
import models.common.{OverseasPropertyModel, PropertyModel}
import models.status.MandationStatusModel
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.*
import services.JsonAuditModel
import utilities.AccountingPeriodUtil

object ITSASignUpSubmissionRequestAuditing {

  private val ITSASignUpSubmissionRequestAudit: String = "ITSASignUpSubmissionRequest"
  private val ukPropertyIncomeSource = "ukProperty"
  private val overseasPropertyIncomeSource = "foreignProperty"
  private val selfEmploymentIncomeSource = "selfEmployment"

  case class ITSASignUpSubmissionRequestAuditModel(agentReferenceNumber: Option[String],
                                                   utr: Option[String],
                                                   nino: Option[String],
                                                   eligibility: Option[EligibilityStatus],
                                                   maybeItsaStatusModel: Option[MandationStatusModel],
                                                   selfEmployments: Seq[SelfEmploymentData],
                                                   maybePropertyModel: Option[PropertyModel],
                                                   maybeOverseasPropertyModel: Option[OverseasPropertyModel])
    extends JsonAuditModel {

    override val auditType: String = ITSASignUpSubmissionRequestAudit

    private val overseasPropertyAsJson: Option[JsValue] = maybeOverseasPropertyModel.map { overseasProperty =>
      val dateLimitYesNo: String = {
        overseasProperty.startDateBeforeLimit match {
          case Some(true) => "Yes"
          case _ => "No"
        }
      }
      Json.toJson(AuditDetailUserPropertyIncome(
        incomeSource = overseasPropertyIncomeSource,
        startDateLimit = Some(AccountingPeriodUtil.getStartDateLimit.toString),
        startDateBeforeLimit = Some(dateLimitYesNo),
        commencementDate = overseasProperty.startDate.map(_.toDesDateFormat)
      ))
    }
    private val ukPropertyAsJson: Option[JsValue] = maybePropertyModel.map { property =>
      val dateLimitYesNo: String = {
        property.startDateBeforeLimit match {
          case Some(true) => "Yes"
          case _ => "No"
        }
      }
      Json.toJson(AuditDetailUserPropertyIncome(
        incomeSource = ukPropertyIncomeSource,
        startDateLimit = Some(AccountingPeriodUtil.getStartDateLimit.toString),
        startDateBeforeLimit = Some(dateLimitYesNo),
        commencementDate = property.startDate.map(_.toDesDateFormat)
      ))
    }

    private val signUpTaxYearsAsJson: JsValue = {
      Json.toJson(AuditDetailSignUpTaxYears(
        currentTaxYear = AccountingPeriodUtil.getCurrentTaxYear.toString,
        nextTaxYear = AccountingPeriodUtil.getNextTaxYear.toString
      ))
    }

    private val itsaStatusAsJson: Option[JsValue] = maybeItsaStatusModel.map { itsaStatus =>
      Json.toJson(AuditDetailItsaStatus(
        currentYearStatus = itsaStatus.currentYearStatus.toString,
        nextYearStatus = itsaStatus.nextYearStatus.toString
      ))
    }

    private val eligibilityAsJson: Option[JsValue] = eligibility.map { eligibilityStatus =>
      val boolToStringCurrent: String = {
        if (eligibilityStatus.eligibleCurrentYear) {
          "Eligible"
        } else {
          "Ineligible"
        }
      }
      val boolToStringNext: String = {
        if (eligibilityStatus.eligibleNextYear) {
          "Eligible"
        } else {
          "Ineligible"
        }
      }
      Json.toJson(AuditDetailEligibilityStatus(
        currentYearStatus = boolToStringCurrent,
        nextYearStatus = boolToStringNext
      ))
    }

    val income: Seq[JsValue] = Seq() ++ ukPropertyAsJson ++ overseasPropertyAsJson ++
      selfEmploymentAsJson(selfEmployments)

    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
    val arn: Option[String] = agentReferenceNumber
    val currentYear: String = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate)

    override val detail: JsValue =
      Json.obj(
        "userType" -> userType,
        "arn" -> arn,
        "nino" -> nino,
        "utr" -> utr,
        "taxYear" -> currentYear,
        "signUpTaxYears" -> signUpTaxYearsAsJson,
        "itsaStatus" -> itsaStatusAsJson,
        "eligibilityStatus" -> eligibilityAsJson,
        "income" -> JsArray(income)
      )

    case class AuditDetailUserPropertyIncome(
                                              incomeSource: String,
                                              startDateLimit: Option[String],
                                              startDateBeforeLimit: Option[String],
                                              commencementDate: Option[String]
                                            )

    object AuditDetailUserPropertyIncome {
      implicit val format: OFormat[AuditDetailUserPropertyIncome] = Json.format[AuditDetailUserPropertyIncome]
    }

    case class AuditDetailSelfEmployedIncome(
                                              incomeSource: String,
                                              businesses: Seq[AuditDetailBusinessIncome]
                                            )

    object AuditDetailSelfEmployedIncome {
      implicit val format: OFormat[AuditDetailSelfEmployedIncome] = Json.format[AuditDetailSelfEmployedIncome]
    }


    case class AuditDetailSignUpTaxYears(
                                          currentTaxYear: String,
                                          nextTaxYear: String
                                        )

    object AuditDetailSignUpTaxYears {
      implicit val format: OFormat[AuditDetailSignUpTaxYears] = Json.format[AuditDetailSignUpTaxYears]
    }

    case class AuditDetailItsaStatus(
                                      currentYearStatus: String,
                                      nextYearStatus: String
                                    )

    object AuditDetailItsaStatus {
      implicit val format: OFormat[AuditDetailItsaStatus] = Json.format[AuditDetailItsaStatus]
    }

    case class AuditDetailEligibilityStatus(
                                             currentYearStatus: String,
                                             nextYearStatus: String
                                           )

    object AuditDetailEligibilityStatus {
      implicit val format: OFormat[AuditDetailEligibilityStatus] = Json.format[AuditDetailEligibilityStatus]
    }

    private def selfEmploymentAsJson(
                                      selfEmployments: Seq[SelfEmploymentData]
                                    ): Option[JsValue] =
      selfEmployments match {
        case Seq() => None
        case _ => Some(
          Json.toJson(AuditDetailSelfEmployedIncome(
            incomeSource = selfEmploymentIncomeSource,
            businesses = selfEmployments.map(se => AuditDetailBusinessIncome(
              businessName = se.businessName.map(_.businessName),
              businessCommencementDate = se.businessStartDate.map(_.startDate.toDesDateFormat),
              businessTrade = se.businessTradeName.map(_.businessTradeName),
              businessAddress = se.businessAddress.map(_.address)
            ))
          )))
      }
  }
}
