package models.audits

import models.EligibilityStatus
import models.DateModel
import models.audits.ITSASignUpSubmissionRequestAuditing.ITSASignUpSubmissionRequestAuditModel
import models.common._
import models.common.business._
import models.status.{MandationStatus, MandationStatusModel}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utilities.AccountingPeriodUtil
import play.api.libs.json._

class ITSASignUpSubmissionRequestAuditModelSpec extends AnyWordSpec with Matchers {

  "ITSASignUpSubmissionRequestAuditModel" should {

    "generate the expected JSON when all data is supplied" in {

      val model = ITSASignUpSubmissionRequestAuditModel(
        agentReferenceNumber = Some("TARN1234567"),
        utr = Some("1234567890"),
        nino = Some("AA123456A"),
        eligibility = Some(
          EligibilityStatus(
            eligibleCurrentYear = true,
            eligibleNextYear = false,
            exemptionReason = None
          )
        ),
        maybeItsaStatusModel = Some(
          MandationStatusModel(
            currentYearStatus = MandationStatus.Voluntary,
            nextYearStatus = MandationStatus.Mandated
          )
        ),
        selfEmployments = Seq(
          SelfEmploymentData(
            id = "business-1",
            startDateBeforeLimit = Some(false),
            businessStartDate = Some(
              BusinessStartDate(DateModel("01", "01", "2022"))
            ),
            businessName = Some(
              BusinessNameModel("ABC Builders")
            ),
            businessTradeName = Some(
              BusinessTradeNameModel("Builder")
            ),
            businessAddress = Some(
              BusinessAddressModel(
                Address(lines = Seq("line 1", "line 2"), postcode = Some("testPostcode"), country = Some(Country("GB", "United Kingdom")))
              )
            ),
            confirmed = true
          )
        ),
        maybePropertyModel = Some(
          PropertyModel(
            startDateBeforeLimit = Some(false),
            startDate = Some(DateModel("06", "04", "2024")),
            confirmed = true
          )
        ),
        maybeOverseasPropertyModel = Some(
          OverseasPropertyModel(
            startDateBeforeLimit = Some(true),
            startDate = None,
            confirmed = true
          )
        )
      )

      val expectedJson =
        Json.obj(
          "userType" -> "agent",
          "arn" -> "TARN1234567",
          "nino" -> "AA123456A",
          "utr" -> "1234567890",
          "taxYear" -> AccountingPeriodUtil.getCurrentTaxYear.toString,
          "signUpTaxYears" -> Json.obj(
            "currentTaxYear" -> AccountingPeriodUtil.getCurrentTaxYear.toString,
            "nextTaxYear" -> AccountingPeriodUtil.getNextTaxYear.toString
          ),
          "itsaStatus" -> Json.obj(
            "currentYearStatus" -> "Voluntary",
            "nextYearStatus" -> "Mandated"
          ),
          "eligibilityStatus" -> Json.obj(
            "currentYearStatus" -> "Eligible",
            "nextYearStatus" -> "Ineligible"
          ),
          "income" -> Json.arr(

            Json.obj(
              "incomeSource" -> "ukProperty",
              "startDateLimit" -> AccountingPeriodUtil.getStartDateLimit.toString,
              "startDateBeforeLimit" -> "No",
              "commencementDate" -> "2024-04-06"
            ),

            Json.obj(
              "incomeSource" -> "foreignProperty",
              "startDateLimit" -> AccountingPeriodUtil.getStartDateLimit.toString,
              "startDateBeforeLimit" -> "Yes"
            ),

            Json.obj(
              "incomeSource" -> "selfEmployment",
              "businesses" -> Json.arr(
                Json.obj(
                  "businessName" -> "ABC Builders",
                  "businessCommencementDate" -> "2022-01-01",
                  "businessTrade" -> "Builder",
                  "businessAddress" -> Json.toJson(
                    Address(
                      lines = Seq("line 1", "line 2"),
                      postcode = Some("testPostcode"),
                      country = Some(Country("GB", "United Kingdom"))
                    )
                  )
                )
              )
            )
          )
        )

      model.detail shouldBe expectedJson
    }

    "generate an individual user when no ARN is supplied" in {

      val model = ITSASignUpSubmissionRequestAuditModel(
        None,
        None,
        None,
        None,
        None,
        Seq.empty,
        None,
        None
      )

      (model.detail \ "userType").as[String] shouldBe "individual"
    }

    "generate an empty income array when no income sources exist" in {

      val model = ITSASignUpSubmissionRequestAuditModel(
        None,
        None,
        None,
        None,
        None,
        Seq.empty,
        None,
        None
      )

      (model.detail \ "income").as[JsArray].value shouldBe Seq.empty
    }

  }

}