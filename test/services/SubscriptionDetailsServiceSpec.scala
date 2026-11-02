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

package services

import models.*
import models.common.{AccountingYearModel, SoleTraderBusiness, SoleTraderBusinesses}
import models.common.business.{BusinessNameModel, SelfEmploymentData}
import models.status.MandationStatus.{Mandated, Voluntary}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{Format, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import services.mocks.{MockGetEligibilityStatusService, MockIncomeTaxSubscriptionConnector, MockMandationStatusService, MockSessionDataService}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

class SubscriptionDetailsServiceSpec extends PlaySpec
  with Matchers
  with MockMandationStatusService
  with MockIncomeTaxSubscriptionConnector
  with MockGetEligibilityStatusService
  with MockSessionDataService {

  val test: Map[String, String] = Map.empty

  val newTest: Map[String, String] = test ++ Some("" -> "")

  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  implicit val jsonCrypto: Encrypter with Decrypter = crypto.JsonCrypto

  implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  val subscriptionDetailsService: SubscriptionDetailsService = new SubscriptionDetailsService(
    mockIncomeTaxSubscriptionConnector,
    mockMandationStatusService,
    mockGetEligibilityStatusService,
    mockSessionDataService,
    crypto
  )

  val testReference = "test-reference"

  "mock Subscription Details service" should {
    "return next year when the ELIGIBLE_NEXT_YEAR_ONLY session variable is set" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Voluntary, Voluntary)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None))

      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe false
        testResult.get.accountingYear mustBe Next
      })
    }

    "return empty db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set and no value found" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Voluntary, Voluntary)


      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe true
      })
    }

    "return editable db value when the ELIGIBLE_NEXT_YEAR_ONLY session variable is not set" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(Some(AccountingYearModel(Current)))
      mockGetMandationService(Voluntary, Voluntary)

      val testResultEventually = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)
      testResultEventually.map(testResult => {
        testResult.isEmpty mustBe false
        testResult.get.confirmed mustBe true
        testResult.get.editable mustBe true
        testResult.get.accountingYear mustBe Next
      })
    }

    "return next year when both ELIGIBLE_NEXT_YEAR_ONLY and mandated current year are true" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(Some(AccountingYearModel(Current)))
      mockGetMandationService(Mandated, Mandated)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))

      val testResult = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)

      testResult.map { result =>
        result.isEmpty mustBe false
        result.get.accountingYear mustBe Next
        result.get.confirmed mustBe true
        result.get.editable mustBe false
      }
    }

    "return current year when mandated current year is true and not eligible next year only" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear)(None)
      mockGetMandationService(Mandated, Mandated)
      mockGetEligibilityStatus(EligibilityStatus(eligibleCurrentYear = true, eligibleNextYear = false, exemptionReason = None))

      val testResult = subscriptionDetailsService.fetchSelectedTaxYear(testReference)(hc)

      testResult.map { result =>
        result.isEmpty mustBe false
        result.get.accountingYear mustBe Current
        result.get.editable mustBe false
      }
    }
  }

  "getAllSubscriptionData" should {
    "return Some(SubscriptionData) when getAllSubscriptionDetails returns valid data" in {
      val testData = Map(
        "key1" -> Json.obj("field1" -> "value1"),
        "key2" -> Json.obj("field2" -> "value2")
      )
      mockGetAllSubscriptionDetails(Some(testData))

      val result = subscriptionDetailsService.getAllSubscriptionData(testReference)(hc)

      result.map { res =>
        res mustBe Some(SubscriptionData(testData))
      }
    }

    "return None when getAllSubscriptionDetails returns None" in {
      mockGetAllSubscriptionDetails(None)

      val result = subscriptionDetailsService.getAllSubscriptionData(testReference)(hc)

      result.map { res =>
        res mustBe None
      }
    }
  }

  "fetchAllSoleTraderBusinesses" should {
    "return the stored businesses" in {
      val businesses = SoleTraderBusinesses(Seq(
        SoleTraderBusiness(id = "business-1", name = Some("Business one"), displayName = Some("Display one"))
      ))
      mockGetSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey)(Some(businesses))

      await(subscriptionDetailsService.fetchAllSoleTraderBusinesses(testReference)(hc)) mustBe businesses
    }

    "return an empty businesses collection when none are stored" in {
      mockGetSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey)(None)

      await(subscriptionDetailsService.fetchAllSoleTraderBusinesses(testReference)(hc)) mustBe SoleTraderBusinesses(Seq.empty)
    }
  }

  "saveBusinesses" should {
    "save sole trader businesses and clear income source confirmation" in {
      val businesses = Seq(
        SoleTraderBusiness(id = "business-1", name = Some("Business one"), displayName = Some("Display one"))
      )
      val storedBusinesses = SoleTraderBusinesses(businesses)
      mockSaveSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey, storedBusinesses)(
        Right(PostSubscriptionDetailsSuccessResponse)
      )
      mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(
        Right(connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse)
      )

      await(subscriptionDetailsService.saveBusinesses(testReference, businesses)(hc)) mustBe
        Right(PostSubscriptionDetailsSuccessResponse)
    }
  }

  "saveBusinessesFromSelfEmploymentData" should {
    "convert and save self-employment data" in {
      val selfEmploymentData = Seq(SelfEmploymentData(
        id = "business-1",
        businessName = Some(BusinessNameModel("Business one")),
        businessDisplayName = Some("Display one")
      ))
      val storedBusinesses = SoleTraderBusinesses(Seq(
        SoleTraderBusiness(id = "business-1", name = Some("Business one"), displayName = Some("Display one"))
      ))
      mockSaveSubscriptionDetails(SubscriptionDataKeys.SoleTraderBusinessesKey, storedBusinesses)(
        Right(PostSubscriptionDetailsSuccessResponse)
      )
      mockDeleteSubscriptionDetails(SubscriptionDataKeys.IncomeSourceConfirmation)(
        Right(connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse)
      )

      await(subscriptionDetailsService.saveBusinessesFromSelfEmploymentData(testReference, selfEmploymentData)(hc)) mustBe
        Right(PostSubscriptionDetailsSuccessResponse)
    }

    "return a save failure without clearing income source confirmation" in {
      val selfEmploymentData = Seq(SelfEmploymentData(id = "business-1"))
      mockSaveSubscriptionDetails(
        SubscriptionDataKeys.SoleTraderBusinessesKey,
        SoleTraderBusinesses(Seq(SoleTraderBusiness(id = "business-1")))
      )(Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

      await(subscriptionDetailsService.saveBusinessesFromSelfEmploymentData(testReference, selfEmploymentData)(hc)) mustBe
        Left(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR))
    }
  }

  lazy val date: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

}
