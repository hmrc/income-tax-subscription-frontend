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

import config.{AppConfig, MockConfig}
import models.common.business.*
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel, SoleTraderBusinesses as StoredSoleTraderBusinesses}
import models.{Current, DateModel, EligibilityStatus, Next, SessionData, SubscriptionData}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.GetCompleteDetailsService.*
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil
import utilities.SubscriptionDataKeys.{OverseasProperty, Property, SelectedTaxYear, SoleTraderBusinessesKey}
import common.Constants.ITSASessionKeys
import models.status.MandationStatus.{Mandated, Voluntary}
import models.status.MandationStatusModel

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetCompleteDetailsServiceSpec extends PlaySpec with Matchers with GuiceOneAppPerSuite with MockSubscriptionDetailsService {

  val appConfig: AppConfig = MockConfig
  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  implicit val jsonCrypto: Encrypter with Decrypter = crypto.JsonCrypto
  implicit val sensitiveFormat: Format[SensitiveString] = JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

  trait Setup {
    val service: GetCompleteDetailsService = new GetCompleteDetailsService(
      subscriptionDetailsService = mockSubscriptionDetailsService,
      applicationCrypto = crypto
    )
  }

  val sessionData: SessionData = SessionData()

  val hc: HeaderCarrier = HeaderCarrier()

  val startDateLimit: LocalDate = AccountingPeriodUtil.getStartDateLimit

  val selfEmployment: SelfEmploymentData = SelfEmploymentData(
    id = "test-id",
    startDateBeforeLimit = Some(false),
    businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(startDateLimit))),
    businessName = Some(BusinessNameModel("ABC Limited")),
    businessDisplayName = Some("ABC Limited"),
    businessTradeName = Some(BusinessTradeNameModel("Plumbing")),
    businessAddress = Some(BusinessAddressModel(address = Address(
      lines = Seq("1 Long Road", "Lonely city"),
      postcode = Some("ZZ11ZZ"),
      country = Some(Country("GB", "United Kingdom"))
    ))),
    confirmed = true
  )

  val ukProperty: PropertyModel = PropertyModel(
    startDate = Some(DateModel.dateConvert(startDateLimit.plusDays(1))),
    confirmed = true
  )

  val foreignProperty: OverseasPropertyModel = OverseasPropertyModel(
    startDate = Some(DateModel.dateConvert(startDateLimit.plusDays(2))),
    confirmed = true
  )

  val accountingYear: AccountingYearModel = AccountingYearModel(Current)

  val completeDetails: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        businesses = Seq(SoleTraderBusiness(
          id = "test-id",
          name = "ABC Limited",
          displayName = Some("ABC Limited"),
          trade = "Plumbing",
          startDate = Some(startDateLimit),
          address = Address(
            lines = Seq("1 Long Road", "Lonely city"),
            postcode = Some("ZZ11ZZ"),
            country = Some(Country("GB", "United Kingdom"))
          )
        ))
      )),
      ukProperty = Some(UKProperty(
        startDate = Some(startDateLimit.plusDays(1))
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = Some(startDateLimit.plusDays(2))
      ))
    ),
    taxYear = AccountingYearModel(Current)
  )

  val completeDetailsNoDates: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = Some(SoleTraderBusinesses(
        businesses = Seq(SoleTraderBusiness(
          id = "test-id",
          name = "ABC Limited",
          displayName = Some("ABC Limited"),
          trade = "Plumbing",
          startDate = None,
          address = Address(
            lines = Seq("1 Long Road", "Lonely city"),
            postcode = Some("ZZ11ZZ"),
            country = Some(Country("GB", "United Kingdom"))
          )
        ))
      )),
      ukProperty = Some(UKProperty(
        startDate = None
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = None
      ))
    ),
    taxYear = AccountingYearModel(Current)
  )

  val completeDetailsNoSoleTraders: CompleteDetails = CompleteDetails(
    incomeSources = IncomeSources(
      soleTraderBusinesses = None,
      ukProperty = Some(UKProperty(
        startDate = Some(startDateLimit.plusDays(1))
      )),
      foreignProperty = Some(ForeignProperty(
        startDate = Some(startDateLimit.plusDays(2))
      ))
    ),
    taxYear = AccountingYearModel(Current)
  )

  private def buildSubscriptionData(selfEmployments: Seq[SelfEmploymentData] = Seq.empty,
                                    ukProperty: Option[PropertyModel] = None,
                                    foreignProperty: Option[OverseasPropertyModel] = None,
                                    selectedTaxYear: Option[AccountingYearModel] = None): SubscriptionData = {
    implicit val encryptedFormat: OFormat[StoredSoleTraderBusinesses] = StoredSoleTraderBusinesses.encryptedFormat

    val entries: Seq[(String, JsValue)] = Seq(
      Option.when(selfEmployments.nonEmpty)(
        SoleTraderBusinessesKey -> Json.toJson(StoredSoleTraderBusinesses(selfEmployments.map(_.toSoleTraderBusiness)))
      ),
      ukProperty.map(Property -> Json.toJson(_)),
      foreignProperty.map(OverseasProperty -> Json.toJson(_)),
      selectedTaxYear.map(SelectedTaxYear -> Json.toJson(_))
    ).flatten

    SubscriptionData(entries.toMap)
  }

  "getCompleteSignUpDetails" must {
    "return a complete details model" when {
      "all fetches were successful and are full + confirmed data sets" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment),
          ukProperty = Some(ukProperty),
          foreignProperty = Some(foreignProperty),
          selectedTaxYear = Some(accountingYear)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetails)
      }
      "all fetches were successful and session data says the user is eligible next year only" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment),
          ukProperty = Some(ukProperty),
          foreignProperty = Some(foreignProperty),
          selectedTaxYear = Some(accountingYear)
        )))

        val sessionData = SessionData(Map(
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(eligibleCurrentYear = false, eligibleNextYear = true, exemptionReason = None))
        ))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetails.copy(taxYear = AccountingYearModel(Next, confirmed = true, editable = false)))
      }
      "all fetches were successful and session data says the user is mandated for the current year" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment),
          ukProperty = Some(ukProperty),
          foreignProperty = Some(foreignProperty),
          selectedTaxYear = Some(AccountingYearModel(Next))
        )))

        val sessionData = SessionData(Map(
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Mandated, Voluntary))
        ))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetails.copy(taxYear = AccountingYearModel(Current, confirmed = true, editable = false)))
      }
      "all fetches were successful and all have selected their start dates are before the limit" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment.copy(startDateBeforeLimit = Some(true))),
          ukProperty = Some(ukProperty.copy(startDateBeforeLimit = Some(true))),
          foreignProperty = Some(foreignProperty.copy(startDateBeforeLimit = Some(true))),
          selectedTaxYear = Some(accountingYear)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetailsNoDates)
      }
      "all fetches were successful, all have selected their start dates are after the limit, but their stored date is older than the limit" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment.copy(
            startDateBeforeLimit = Some(false),
            businessStartDate = Some(BusinessStartDate(DateModel.dateConvert(startDateLimit.minusDays(1))))
          )),
          ukProperty = Some(ukProperty.copy(
            startDateBeforeLimit = Some(false),
            startDate = Some(DateModel.dateConvert(startDateLimit.minusDays(1)))
          )),
          foreignProperty = Some(foreignProperty.copy(
            startDateBeforeLimit = Some(false),
            startDate = Some(DateModel.dateConvert(startDateLimit.minusDays(1)))
          )),
          selectedTaxYear = Some(accountingYear)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetailsNoDates)
      }
      "all fetches were successful and all have selected their start dates are before the limit and no dates are stored" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment.copy(
            startDateBeforeLimit = Some(true),
            businessStartDate = None
          )),
          ukProperty = Some(ukProperty.copy(
            startDateBeforeLimit = Some(true),
            startDate = None
          )),
          foreignProperty = Some(foreignProperty.copy(
            startDateBeforeLimit = Some(true),
            startDate = None
          )),
          selectedTaxYear = Some(accountingYear)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetailsNoDates)
      }
      "no self employments are provided" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          ukProperty = Some(ukProperty),
          foreignProperty = Some(foreignProperty),
          selectedTaxYear = Some(accountingYear)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Right(completeDetailsNoSoleTraders)
      }
    }

    "return a GetCompleteDetailsFailure" when {
      "all income sources are complete" when {
        "self employment hasn't been confirmed" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment.copy(confirmed = false)),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "uk property hasn't been confirmed" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment),
            ukProperty = Some(ukProperty.copy(confirmed = false)),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "foreign property hasn't been confirmed" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty.copy(confirmed = false)),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "foreign property hasn't been confirmed when there is no uk property" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment),
            foreignProperty = Some(foreignProperty.copy(confirmed = false)),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }

      "there is no selected tax year" in new Setup {
        mockGetAllSubscriptionData(Some(buildSubscriptionData(
          selfEmployments = Seq(selfEmployment),
          ukProperty = Some(ukProperty),
          foreignProperty = Some(foreignProperty)
        )))

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Left(GetCompleteDetailsFailure)
      }
      "there is no subscription data" in new Setup {
        mockGetAllSubscriptionData(None)

        val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
          service.getCompleteSignUpDetails("reference", sessionData)(hc)
        }

        await(result) mustBe Left(GetCompleteDetailsFailure)
      }
      "there is a sole trader business" which {
        "does not have a name" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment.copy(businessName = None)),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a trade" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment.copy(businessTradeName = None)),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have a start date" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment.copy(businessStartDate = None)),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
        "does not have an address" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment.copy(businessAddress = None)),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a uk property business" which {
        "does not have a start date" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment),
            ukProperty = Some(ukProperty.copy(startDate = None)),
            foreignProperty = Some(foreignProperty),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
      "there is a foreign property business" which {
        "does not have a start date" in new Setup {
          mockGetAllSubscriptionData(Some(buildSubscriptionData(
            selfEmployments = Seq(selfEmployment),
            ukProperty = Some(ukProperty),
            foreignProperty = Some(foreignProperty.copy(startDate = None)),
            selectedTaxYear = Some(accountingYear)
          )))

          val result: Future[Either[GetCompleteDetailsService.GetCompleteDetailsFailure.type, GetCompleteDetailsService.CompleteDetails]] = {
            service.getCompleteSignUpDetails("reference", sessionData)(hc)
          }

          await(result) mustBe Left(GetCompleteDetailsFailure)
        }
      }
    }
  }

}
