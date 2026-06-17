package models.audits

import models.common.business.SelfEmploymentData
import models.common.{OverseasPropertyModel, PropertyModel}
import models.status.MandationStatusModel
import play.api.libs.json.{JsArray, JsValue}
import services.AuditModel
import utilities.AccountingPeriodUtil

object ITSASignUpSubmissionRequestAuditing {

  case class ITSASignUpSubmissionRequestAuditModel(agentReferenceNumber: Option[String],
                                                   utr: Option[String],
                                                   nino: Option[String],
                                                   eligibility: Option[EligibilityStatus],
                                                   maybeItsaStatusModel: Option[MandationStatusModel],
                                                   selfEmployments: Seq[SelfEmploymentData],
                                                   maybePropertyModel: Option[PropertyModel],
                                                   maybeOverseasPropertyModel: Option[OverseasPropertyModel])
    extends JsonAuditModel {


    private val overseasPropertyAsJson: Option[JsValue] = maybeOverseasPropertyModel.map { overseasProperty =>
      Json.toJson(AuditDetailPropertyIncome(
        incomeSource = overseasPropertyIncomeSource,
        startDateLimit = AccountingPeriodUtil.getStartDateLimit,
        startDateBeforeLimit = overseasProperty.startDateBeforeLimit,
        commencementDate = overseasProperty.startDate.map(_.toDesDateFormat)
      ))
    }
    private val ukPropertyAsJson: Option[JsValue] = maybePropertyModel.map { property =>
      Json.toJson(AuditDetailPropertyIncome(
        incomeSource = ukPropertyIncomeSource,
        startDateLimit = AccountingPeriodUtil.getStartDateLimit,
        startDateBeforeLimit = property.startDateBeforeLimit,
        commencementDate = property.startDate.map(_.toDesDateFormat)
      ))
    }

    private val signUpTaxYearsAsJson: Option[JsValue] = { signUpTaxYears =>
      Json.toJson(AuditDetailPropertyIncome(
        currentTaxYear = AccountingPeriodUtil.getCurrentTaxYear.toString,
        nextTaxYear = AccountingPeriodUtil.getNextTaxYear.toString
      ))
    }

    private val itsaStatusAsJson: Option[JsValue] = maybeItsaStatusModel.map { itsaStatus =>
      Json.toJson(AuditDetailItsaStatus(
        currentYearStatus = itsaStatus.currentYearStatus,
        nextYearStatus = itsaStatus.nextYearStatus
      ))
    }

    private val elibilityAsJson: Option[JsValue] = eligibility.map { eligibilityStatus =>
      Json.toJson(AuditDetailEligibilityStatus(
        currentYearStatus = eligibilityStatus.currentYearStatus,
        nextYearStatus = eligibilityStatus.nextYearStatus
      ))
    }

    val income: Seq[JsValue] = Seq() ++ ukPropertyAsJson ++ overseasPropertyAsJson ++
      selfEmploymentAsJson(selfEmployments)

    override val transactionName: Option[String] = None
    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
    val arn: Option[String] = agentReferenceNumber
    val nino: Option[String] = nino
    val utr: Option[String] = utr
    val currentYear: String = AccountingPeriodUtil.getCurrentTaxYear.toString

    override val detail: Map[String, String] = Map(
      "userType" -> userType,
      "arn" -> arn,
      "nino" -> nino,
      "utr" -> utr,
      "taxYear" -> currentYear,
      "signUpTaxYears" -> signUpTaxYearsAsJson,
      "itsaStatus" -> itsaStatusAsJson,
      "eligibilityStatus" -> elibilityAsJson,
      "income" -> JsArray(income)
    )

    override val auditType: String = itsaSignUpSubmissionRequestType
  }
}

private def selfEmploymentAsJson(
                                  selfEmployments: Seq[SelfEmploymentData]
                                ): Option[JsValue] =
  selfEmployments match {
    case Seq() => None
    case _ => Some(
      Json.toJson(AuditDetailSelfEmploymentIncome(
        incomeSource = selfEmploymentIncomeSource,
        numberOfBusinesses = s"${
          selfEmployments.size
        }",
        businesses = selfEmployments.map(se => AuditDetailBusinessIncome(
          businessName = se.businessName.map(_.businessName),
          businessCommencementDate = se.businessStartDate.map(_.startDate.toDesDateFormat),
          businessTrade = se.businessTradeName.map(_.businessTradeName),
          businessAddress = se.businessAddress.map(_.address)
        ))
      )))
  }

  case class AuditDetailPropertyIncome(
                                        incomeSource: String,
                                        startDateLimit: Option[String],
                                        startDateBeforeLimit: Boolean,
                                        commencementDate: Option[String]
                                      )

  object AuditDetailPropertyIncome {
    implicit val format: OFormat[AuditDetailPropertyIncome] = Json.format[AuditDetailPropertyIncome]
  }

  case class AuditDetailSelfEmploymentIncome(
                                              incomeSource: String,
                                              numberOfBusinesses: String,
                                              businesses: Seq[AuditDetailBusinessIncome]
                                            )

  object AuditDetailSelfEmploymentIncome {
    implicit val format: OFormat[AuditDetailSelfEmploymentIncome] = Json.format[AuditDetailSelfEmploymentIncome]
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
