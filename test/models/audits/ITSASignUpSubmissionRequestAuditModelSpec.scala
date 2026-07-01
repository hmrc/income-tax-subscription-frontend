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
import play.api.libs.json.{JsObject, Json}
import services.GetCompleteDetailsService.*
import utilities.AccountingPeriodUtil

import java.time.LocalDate

class ITSASignUpSubmissionRequestAuditModelSpec extends AnyWordSpec with Matchers {

  // ----- Shared fixtures -----

  private val defaultEligibility = EligibilityStatus(
    eligibleCurrentYear = true,
    eligibleNextYear = false,
    exemptionReason = None
  )

  private val defaultItsaStatus = MandationStatusModel(
    currentYearStatus = MandationStatus.Voluntary,
    nextYearStatus = MandationStatus.Mandated
  )

  private val soleTraderBusiness = SoleTraderBusiness(
    id = "business-1",
    name = "ABC Builders",
    trade = "Builder",
    startDate = Some(LocalDate.of(2022, 1, 1)),
    address = Address(
      lines = Seq("line 1", "line 2"),
      postcode = Some("testPostcode"),
      country = Some(Country("GB", "United Kingdom"))
    )
  )

  private val populatedCompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(Seq(soleTraderBusiness))),
      ukProperty = Some(UKProperty(startDate = Some(LocalDate.of(2024, 4, 6)))),
      foreignProperty = Some(ForeignProperty(startDate = None))
    ),
    taxYear = AccountingYearModel(Current)
  )

  private val emptyCompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = None,
      ukProperty = None,
      foreignProperty = None
    ),
    taxYear = AccountingYearModel(Current)
  )

  /** Canonical agent model with all income sources populated */
  private val agentModel = ITSASignUpSubmissionRequestAuditModel(
    agentReferenceNumber = Some("TARN1234567"),
    utr = "1234567890",
    nino = "AA123456A",
    eligibility = defaultEligibility,
    itsaStatus = defaultItsaStatus,
    completeDetails = populatedCompleteDetails
  )

  /** Canonical individual model with no income sources */
  private val individualModel = ITSASignUpSubmissionRequestAuditModel(
    agentReferenceNumber = None,
    utr = "1234567890",
    nino = "AA123456A",
    eligibility = defaultEligibility,
    itsaStatus = defaultItsaStatus,
    completeDetails = emptyCompleteDetails
  )

  /** Builds a minimal model with the given income sources, leaving all other fields at defaults */
  private def modelWith(incomeSources: IncomeSources): ITSASignUpSubmissionRequestAuditModel =
    ITSASignUpSubmissionRequestAuditModel(
      agentReferenceNumber = None,
      utr = "",
      nino = "",
      eligibility = defaultEligibility,
      itsaStatus = defaultItsaStatus,
      completeDetails = CompleteDetails(incomeSources = incomeSources, taxYear = AccountingYearModel(Current))
    )

  /** Extracts the top-level incomeSources object from the model's audit detail */
  private def incomeSourcesOf(model: ITSASignUpSubmissionRequestAuditModel): JsObject =
    (model.detail \\ "incomeSources").head.as[JsObject]

  // ----- Tests -----

  "ITSASignUpSubmissionRequestAuditModel" when {

    "checking audit metadata" should {

      "have auditType set to ITSASignUpSubmissionRequest" in {
        agentModel.auditType shouldBe "ITSASignUpSubmissionRequest"
      }
    }

    "checking the userType field" should {

      "be 'agent' when an agentReferenceNumber is supplied" in {
        (agentModel.detail \ "userType").as[String] shouldBe "agent"
      }

      "be 'individual' when no agentReferenceNumber is supplied" in {
        (individualModel.detail \ "userType").as[String] shouldBe "individual"
      }
    }

    "checking the agentReferenceNumber field" should {

      "include agentReferenceNumber in the JSON when one is supplied" in {
        (agentModel.detail \ "agentReferenceNumber").as[String] shouldBe "TARN1234567"
      }

      "omit agentReferenceNumber from the JSON when none is supplied" in {
        (individualModel.detail \ "agentReferenceNumber").asOpt[String] shouldBe None
      }
    }

    "producing the full JSON payload" should {

      "generate the complete expected JSON for an agent with all income sources" in {
        agentModel.detail shouldBe Json.obj(
          "userType" -> "agent",
          "agentReferenceNumber" -> "TARN1234567",
          "nino" -> "AA123456A",
          "utr" -> "1234567890",
          "taxYear" -> populatedCompleteDetails.taxYear.toFullYearFormat,
          "signUpTaxYears" -> Json.obj(
            "currentYear" -> AccountingPeriodUtil.getCurrentTaxYear.toFullTaxYear,
            "nextYear" -> AccountingPeriodUtil.getNextTaxYear.toFullTaxYear
          ),
          "itsaStatus" -> Json.obj(
            "currentYearStatus" -> "Voluntary",
            "nextYearStatus" -> "Mandated"
          ),
          "eligibilityStatus" -> Json.obj(
            "currentYearStatus" -> "Eligible",
            "nextYearStatus" -> "Ineligible"
          ),
          "incomeSources" -> Json.obj(
            "ukProperty" -> Json.obj(
              "startDateLimit" -> AccountingPeriodUtil.getStartDateLimit.toString,
              "startDateBeforeLimit" -> "no",
              "startDate" -> "2024-04-06"
            ),
            "foreignProperty" -> Json.obj(
              "startDateLimit" -> AccountingPeriodUtil.getStartDateLimit.toString,
              "startDateBeforeLimit" -> "yes"
            ),
            "selfEmployment" -> Json.arr(
              Json.obj(
                "startDateLimit" -> AccountingPeriodUtil.getStartDateLimit.toString,
                "startDateBeforeLimit" -> "no",
                "startDate" -> "2022-01-01",
                "trade" -> "Builder",
                "name" -> "ABC Builders",
                "address" -> Json.toJson(soleTraderBusiness.address)
              )
            )
          )
        )
      }
    }

    "handling income sources" should {

      "produce an empty incomeSources object when no income sources are present" in {
        incomeSourcesOf(individualModel).value.isEmpty shouldBe true
      }

      "include only ukProperty when only a UK property income source is present" in {
        val model = modelWith(IncomeSources(
          soleTraderBusinesses = None,
          ukProperty = Some(UKProperty(startDate = Some(LocalDate.of(2024, 4, 6)))),
          foreignProperty = None
        ))
        incomeSourcesOf(model).keys shouldBe Set("ukProperty")
      }

      "include only selfEmployment when only sole trader businesses are present" in {
        val model = modelWith(IncomeSources(
          soleTraderBusinesses = Some(SoleTraderBusinesses(Seq(
            SoleTraderBusiness(id = "b1", name = "Name", trade = "Trade", startDate = Some(LocalDate.of(2022, 1, 1)), address = Address(Seq("l1"), None, None))
          ))),
          ukProperty = None,
          foreignProperty = None
        ))
        incomeSourcesOf(model).keys shouldBe Set("selfEmployment")
      }

      "include only foreignProperty when only a foreign property income source is present" in {
        val model = modelWith(IncomeSources(
          soleTraderBusinesses = None,
          ukProperty = None,
          foreignProperty = Some(ForeignProperty(startDate = Some(LocalDate.of(2025, 5, 5))))
        ))
        incomeSourcesOf(model).keys shouldBe Set("foreignProperty")
      }
    }

    "handling the startDate block within an income source" should {

      "set startDateBeforeLimit to 'no' and include startDate when a start date is provided" in {
        val ukPropertyBlock = (agentModel.detail \\ "ukProperty").head.as[JsObject]
        (ukPropertyBlock \ "startDateBeforeLimit").as[String] shouldBe "no"
        (ukPropertyBlock \ "startDate").as[String] shouldBe "2024-04-06"
      }

      "set startDateBeforeLimit to 'yes' and omit startDate when no start date is provided" in {
        val foreignPropertyBlock = (agentModel.detail \\ "foreignProperty").head.as[JsObject]
        (foreignPropertyBlock \ "startDateBeforeLimit").as[String] shouldBe "yes"
        (foreignPropertyBlock \ "startDate").asOpt[String] shouldBe None
      }

      "format all startDate values as yyyy-MM-dd" in {
        (agentModel.detail \\ "startDate").map(_.as[String]).foreach { dateStr =>
          dateStr should fullyMatch regex "\\d{4}-\\d{2}-\\d{2}"
        }
      }
    }

    "handling the address block within a sole trader business" should {

      "include uprn in the address JSON when one is present" in {
        val businessWithUprn = soleTraderBusiness.copy(
          address = soleTraderBusiness.address.copy(uprn = Some("UPRN-123"))
        )
        val model = modelWith(IncomeSources(
          soleTraderBusinesses = Some(SoleTraderBusinesses(Seq(businessWithUprn))),
          ukProperty = None,
          foreignProperty = None
        ))
        (model.detail \\ "uprn").head.as[String] shouldBe "UPRN-123"
      }

      "omit uprn from the address JSON when none is present" in {
        (agentModel.detail \\ "uprn").headOption shouldBe None
      }
    }
  }
}