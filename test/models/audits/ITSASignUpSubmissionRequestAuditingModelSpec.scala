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

import models.{Current, EligibilityStatus}
import models.audits.ITSASignUpSubmissionRequestAuditing.ITSASignUpSubmissionRequestAuditModel
import models.common.AccountingYearModel
import models.common.business.{Address, Country}
import models.status.{MandationStatus, MandationStatusModel}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsArray, Json}
import services.GetCompleteDetailsService.*
import utilities.{AccountingPeriodUtil, CurrentDateProvider}

import java.time.LocalDate

class ITSASignUpSubmissionRequestAuditModelSpec extends AnyWordSpec with Matchers {

  private val currentYear = 2023
  private val selectedTaxYear = Some(AccountingYearModel(Current))
  private val populatedCompleteDetails =
    CompleteDetails(
      incomeSources =
        IncomeSources(
          soleTraderBusinesses = Some(
            SoleTraderBusinesses(
              Seq(
                SoleTraderBusiness(
                  id = "business-1",
                  name = "ABC Builders",
                  trade = "Builder",
                  startDate = Some(LocalDate.of(2022, 1, 1)),
                  address =
                    Address(
                      lines = Seq("line 1", "line 2"),
                      postcode = Some("testPostcode"),
                      country = Some(Country("GB", "United Kingdom"))
                    )
                )
              )
            )
          ),
          ukProperty = Some(
            UKProperty(
              startDate = Some(LocalDate.of(2024, 4, 6))
            )
          ),
          foreignProperty = Some(
            ForeignProperty(
              startDate = None
            )
          )
        ),
      taxYear = AccountingYearModel(Current)
    )

  private val emptyCompleteDetails =
    CompleteDetails(
      incomeSources =
        IncomeSources(
          soleTraderBusinesses = None,
          ukProperty = None,
          foreignProperty = None
        ),
      taxYear = AccountingYearModel(Current)
    )

  "ITSASignUpSubmissionRequestAuditModel" should {

    "generate the expected JSON when all data is supplied" in {

      val model =
        ITSASignUpSubmissionRequestAuditModel(
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
          currentYear = 2023,
          maybeItsaStatusModel = Some(
            MandationStatusModel(
              currentYearStatus = MandationStatus.Voluntary,
              nextYearStatus = MandationStatus.Mandated
            )
          ),
          completeDetails = populatedCompleteDetails
        )

      val expectedJson =
        Json.obj(
          "userType" -> "agent",
          "arn" -> "TARN1234567",
          "nino" -> "AA123456A",
          "utr" -> "1234567890",
          "taxYear" -> currentYear.toString,
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

      val model =
        ITSASignUpSubmissionRequestAuditModel(
          agentReferenceNumber = None,
          utr = None,
          nino = None,
          eligibility = None,
          currentYear = currentYear,
          maybeItsaStatusModel = None,
          completeDetails = emptyCompleteDetails
        )

      (model.detail \ "userType").as[String] shouldBe "individual"
    }

    "generate an empty income array when no income sources exist" in {

      val model =
        ITSASignUpSubmissionRequestAuditModel(
          agentReferenceNumber = None,
          utr = None,
          nino = None,
          eligibility = None,
          currentYear = currentYear,
          maybeItsaStatusModel = None,
          completeDetails = emptyCompleteDetails
        )

      (model.detail \ "income").as[JsArray].value shouldBe Seq.empty
    }
  }
}