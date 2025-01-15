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

import models.common.business.{AccountingMethodModel, Address, SelfEmploymentData}
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Current, Next}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import services.JsonAuditModel

object SaveAndComebackAuditing {
  val signUpSaveAndComeBackAudit: String = "SignUpSaveAndComeBack"
  val individualUserType: String = "individual"
  val agentUserType: String = "agent"

  val ukPropertyIncomeSource = "ukProperty"
  val overseasPropertyIncomeSource = "foreignProperty"
  val selfEmploymentIncomeSource = "selfEmployment"

  case class SaveAndComeBackAuditModel(
                                        userType: String,
                                        utr: String,
                                        nino: String,
                                        maybeAgentReferenceNumber: Option[String] = None,
                                        saveAndRetrieveLocation: String,
                                        currentTaxYear: Int,
                                        selectedTaxYear: Option[AccountingYearModel],
                                        selfEmployments: Seq[SelfEmploymentData],
                                        maybeSelfEmploymentAccountingMethod: Option[AccountingMethodModel],
                                        maybePropertyModel: Option[PropertyModel],
                                        maybeOverseasPropertyModel: Option[OverseasPropertyModel]
                                      ) extends JsonAuditModel {
    override val auditType: String = signUpSaveAndComeBackAudit

    private val overseasPropertyAsJson: Option[JsValue] = maybeOverseasPropertyModel.map { overseasProperty =>
      Json.toJson(AuditDetailPropertyIncome(
        incomeSource = overseasPropertyIncomeSource,
        commencementDate = overseasProperty.startDate.map(_.toDesDateFormat),
        accountingType = overseasProperty.accountingMethod.map(_.toString)
      ))
    }
    private val ukPropertyAsJson: Option[JsValue] = maybePropertyModel.map { property =>
      Json.toJson(AuditDetailPropertyIncome(
        incomeSource = ukPropertyIncomeSource,
        commencementDate = property.startDate.map(_.toDesDateFormat),
        accountingType = property.accountingMethod.map(_.toString)
      ))
    }
    val income: Seq[JsValue] = Seq() ++ ukPropertyAsJson ++ overseasPropertyAsJson ++
      selfEmploymentAsJson(selfEmployments, maybeSelfEmploymentAccountingMethod)

    override val detail: JsValue = Json.obj(
      "userType" -> userType,
      "saUtr" -> utr,
      "nino" -> nino,
      "saveAndRetrieveLocation" -> saveAndRetrieveLocation,
      "income" -> JsArray(income)
    ) ++ selectedTaxYear.fold(Json.obj()) {
      case AccountingYearModel(Next, _, _) => Json.obj("taxYear" -> s"$currentTaxYear-${currentTaxYear + 1}")
      case AccountingYearModel(Current, _, _) => Json.obj("taxYear" -> s"${currentTaxYear - 1}-$currentTaxYear")
    } ++ maybeAgentReferenceNumber.fold(Json.obj())(agentReferenceNumber => Json.obj("agentReferenceNumber" -> agentReferenceNumber))
  }

  private def selfEmploymentAsJson(
                                    selfEmployments: Seq[SelfEmploymentData],
                                    maybeSelfEmploymentAccountingMethod: Option[AccountingMethodModel]
                                  ): Option[JsValue] =
    selfEmployments match {
      case Seq() => None
      case _ => Some(
        Json.toJson(AuditDetailSelfEmploymentIncome(
          incomeSource = selfEmploymentIncomeSource,
          numberOfBusinesses = s"${
            selfEmployments.size
          }",
          accountingType = maybeSelfEmploymentAccountingMethod.map(_.accountingMethod.toString),
          businesses = selfEmployments.map(se => AuditDetailBusinessIncome(
            businessName = se.businessName.map(_.businessName),
            businessCommencementDate = se.businessStartDate.map(_.startDate.toDesDateFormat),
            businessTrade = se.businessTradeName.map(_.businessTradeName),
            businessAddress = se.businessAddress.map(_.address)
          ))
        )))
    }
}

case class AuditDetailPropertyIncome(
                                      incomeSource: String,
                                      commencementDate: Option[String],
                                      accountingType: Option[String]
                                    )

object AuditDetailPropertyIncome {
  implicit val format: OFormat[AuditDetailPropertyIncome] = Json.format[AuditDetailPropertyIncome]
}

case class AuditDetailSelfEmploymentIncome(
                                            incomeSource: String,
                                            numberOfBusinesses: String,
                                            accountingType: Option[String],
                                            businesses: Seq[AuditDetailBusinessIncome]
                                          )

object AuditDetailSelfEmploymentIncome {
  implicit val format: OFormat[AuditDetailSelfEmploymentIncome] = Json.format[AuditDetailSelfEmploymentIncome]
}

case class AuditDetailBusinessIncome(
                                      businessName: Option[String],
                                      businessCommencementDate: Option[String],
                                      businessTrade: Option[String],
                                      businessAddress: Option[Address]
                                    )

object AuditDetailBusinessIncome {
  implicit val format: OFormat[AuditDetailBusinessIncome] = Json.format[AuditDetailBusinessIncome]
}
