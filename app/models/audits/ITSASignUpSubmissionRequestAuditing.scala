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
import services.GetCompleteDetailsService.{CompleteDetails, SoleTraderBusinesses}
import services.JsonAuditModel
import utilities.{AccountingPeriodUtil, CurrentDateProvider}

object ITSASignUpSubmissionRequestAuditing {

  private val ITSASignUpSubmissionRequestAudit: String = "ITSASignUpSubmissionRequest"
  private val ukPropertyIncomeSource = "ukProperty"
  private val overseasPropertyIncomeSource = "foreignProperty"
  private val selfEmploymentIncomeSource = "selfEmployment"

  case class ITSASignUpSubmissionRequestAuditModel(agentReferenceNumber: Option[String],
                                                   utr: Option[String],
                                                   nino: Option[String],
                                                   eligibility: Option[EligibilityStatus],
                                                   currentYear: Int,
                                                   maybeItsaStatusModel: Option[MandationStatusModel],
                                                   completeDetails: CompleteDetails)
    extends JsonAuditModel {

    override val auditType: String = ITSASignUpSubmissionRequestAudit

    private val overseasPropertyAsJson: Option[JsValue] = completeDetails.incomeSources.foreignProperty.map { overseasProperty =>
      Json.toJson(AuditDetailUserPropertyIncome(
        incomeSource = overseasPropertyIncomeSource,
        startDateLimit = Some(AccountingPeriodUtil.getStartDateLimit.toString),
        startDateBeforeLimit = Some(if (overseasProperty.startDate.isEmpty) "Yes" else "No"),
        commencementDate = overseasProperty.startDate.map(_.toString)
      ))
    }
    private val ukPropertyAsJson: Option[JsValue] = completeDetails.incomeSources.ukProperty.map { property =>
      Json.toJson(AuditDetailUserPropertyIncome(
        incomeSource = ukPropertyIncomeSource,
        startDateLimit = Some(AccountingPeriodUtil.getStartDateLimit.toString),
        startDateBeforeLimit = Some(if (property.startDate.isEmpty) "Yes" else "No"),
        commencementDate = property.startDate.map(_.toString)
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
      selfEmploymentAsJson(completeDetails.incomeSources.soleTraderBusinesses)

    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
    val arn: Option[String] = agentReferenceNumber
    

    override val detail: JsValue =
      Json.obj(
        "userType" -> userType,
        "arn" -> arn,
        "nino" -> nino,
        "utr" -> utr,
        "taxYear" -> currentYear.toString,
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
                                      selfEmployments: Option[SoleTraderBusinesses]
                                    ): Option[JsValue] =
      selfEmployments.map { businesses => 
        Json.toJson(
          AuditDetailSelfEmployedIncome(
            incomeSource =  selfEmploymentIncomeSource,
            businesses = businesses.businesses.map{ business =>
              
              AuditDetailBusinessIncome(
                businessName = Some(business.name),
                businessTrade = Some(business.trade),
                businessAddress =  Some(business.address),
                businessCommencementDate = business.startDate.map(_.toString)
              )
            }
          )
        )
      }
  }
}
