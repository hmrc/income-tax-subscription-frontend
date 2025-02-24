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
import connectors.httpparser.PostSubscriptionDetailsHttpParser
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import connectors.mocks.MockPrePopConnector
import models._
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import models.prepop.{PrePopData, PrePopSelfEmployment}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PrePopDataService.PrePopResult.{PrePopFailure, PrePopSuccess}
import services.mocks.{MockNinoService, MockSubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.testNino
import utilities.{AccountingPeriodUtil, MockUUIDProvider}

import scala.concurrent.ExecutionContext.Implicits.global

class PrePopDataServiceSpec extends PlaySpec
  with Matchers
  with MockNinoService
  with MockPrePopConnector
  with MockSubscriptionDetailsService
  with MockUUIDProvider {

  val appConfig: AppConfig = MockConfig


  val service: PrePopDataService = new PrePopDataService(
    mockNinoService,
    mockPrePopConnector,
    mockSubscriptionDetailsService,
    mockUUIDProvider
  )

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val reference: String = "test-reference"

  "prePopIncomeSources" when {
    "the pre-pop feature switch is enabled" when {
      "the user has previously had their information pre-populated" must {
        "return a pre-pop success response" in {
          mockFetchPrePopFlag(Some(true))

          await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

          verifyFetchPrePopFlag()
        }
      }
      "the user has previously had pre-pop occur but no income sources pre-populated" must {
        "return a pre-pop success response" in {
          mockFetchPrePopFlag(Some(false))

          await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

          verifyFetchPrePopFlag()
        }
      }
      "the user has not had their information prepopulated before" when {
        "return a pre-pop success response" when {
          "the fetched pre-pop data is full and complete income sources and saving of the data was successful" in {
            mockFetchPrePopFlag(None)
            mockGetPrePopData(testNino)(Right(fullPrePopData))
            mockUUID(testUUID)
            mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveBusinesses(
              businesses = Seq(expectedFullSelfEmploymentData, expectedFullSelfEmploymentData),
              accountingMethod = Some(accountingMethod)
            )(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveProperty(expectedUkProperty)(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveOverseasProperty(expectedForeignProperty)(Right(PostSubscriptionDetailsSuccessResponse))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true)
            verifySaveBusinesses(
              businesses = Seq(expectedFullSelfEmploymentData, expectedFullSelfEmploymentData),
              accountingMethod = Some(accountingMethod)
            )
            verifySaveProperty(expectedUkProperty)
            verifySaveOverseasProperty(expectedForeignProperty)
          }
          "the fetched pre-pop data has minimal income source data and saving of the data was successful" in {
            mockFetchPrePopFlag(None)
            mockGetPrePopData(testNino)(Right(minimalPrePopData))
            mockUUID(testUUID)
            mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData),
              accountingMethod = Some(accountingMethod)
            )(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveProperty(expectedUkProperty)(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveOverseasProperty(expectedForeignProperty)(Right(PostSubscriptionDetailsSuccessResponse))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true)
            verifySaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData),
              accountingMethod = Some(accountingMethod)
            )
            verifySaveProperty(expectedUkProperty)
            verifySaveOverseasProperty(expectedForeignProperty)
          }
          "the fetched pre-pop data has only self employment data" which {
            "was saved successfully" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(selfEmploymentOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData),
                accountingMethod = Some(accountingMethod)
              )(Right(PostSubscriptionDetailsSuccessResponse))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData),
                accountingMethod = Some(accountingMethod)
              )
            }
            "returned a save failure when saving" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(selfEmploymentOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData),
                accountingMethod = Some(accountingMethod)
              )(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData),
                accountingMethod = Some(accountingMethod)
              )
            }
          }
          "the fetched pre-pop data has only a uk property accounting method" which {
            "was saved successfully" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(ukPropertyOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveProperty(expectedUkProperty)(Right(PostSubscriptionDetailsSuccessResponse))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveProperty(expectedUkProperty)

            }
            "returned a save failure when saving" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(ukPropertyOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveProperty(expectedUkProperty)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveProperty(expectedUkProperty)
            }
          }
          "the fetched pre-pop data has only a foreign property accounting method" which {
            "was saved successfully" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(foreignPropertyOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveOverseasProperty(expectedForeignProperty)(Right(PostSubscriptionDetailsSuccessResponse))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveOverseasProperty(expectedForeignProperty)
            }
            "returned a save failure when saving" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(foreignPropertyOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveOverseasProperty(expectedForeignProperty)(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveOverseasProperty(expectedForeignProperty)
            }
          }
        }
        "return a pre-pop failure response" when {
          "there was a problem fetching the pre-pop data from the connector" in {
            val error = ErrorModel(INTERNAL_SERVER_ERROR, "Failure")
            mockFetchPrePopFlag(None)
            mockGetPrePopData(testNino)(Left(ErrorModel(INTERNAL_SERVER_ERROR, "Failure")))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopFailure(error.toString)

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true, count = 0)
            verifySaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData),
              accountingMethod = Some(accountingMethod),
              count = 0
            )
            verifySaveProperty(expectedUkProperty, count = 0)
            verifySaveOverseasProperty(expectedForeignProperty, count = 0)
          }
          "there was a problem when saving the pre-pop flag" in {
            val error = PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)

            mockFetchPrePopFlag(None)
            mockGetPrePopData(testNino)(Right(minimalPrePopData))
            mockSavePrePopFlag(flag = true)(Left(error))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopFailure(error.toString)

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true)
            verifySaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData),
              accountingMethod = Some(accountingMethod),
              count = 0
            )
            verifySaveProperty(expectedUkProperty, count = 0)
            verifySaveOverseasProperty(expectedForeignProperty, count = 0)
          }
        }
      }
    }
  }

  lazy val fullPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      fullPrePopSelfEmployment,
      fullPrePopSelfEmployment
    )),
    ukPropertyAccountingMethod = Some(Accruals),
    foreignPropertyAccountingMethod = Some(Cash)
  )

  lazy val minimalPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      minimalPrePopSelfEmployment
    )),
    ukPropertyAccountingMethod = Some(Accruals),
    foreignPropertyAccountingMethod = Some(Cash)
  )

  lazy val selfEmploymentOnlyPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      minimalPrePopSelfEmployment
    )),
    ukPropertyAccountingMethod = None,
    foreignPropertyAccountingMethod = None
  )

  lazy val ukPropertyOnlyPrePopData: PrePopData = PrePopData(
    selfEmployment = None,
    ukPropertyAccountingMethod = Some(Accruals),
    foreignPropertyAccountingMethod = None
  )

  lazy val foreignPropertyOnlyPrePopData: PrePopData = PrePopData(
    selfEmployment = None,
    ukPropertyAccountingMethod = None,
    foreignPropertyAccountingMethod = Some(Cash)
  )

  val name: String = "ABC"
  val trade: String = "Plumbing"
  val address: Address = Address(
    lines = Seq(
      "1 long road"
    ),
    postcode = Some("ZZ1 1ZZ")
  )

  val startDate: DateModel = DateModel.dateConvert(AccountingPeriodUtil.getStartDateLimit)

  lazy val accountingMethod: AccountingMethod = Cash

  lazy val fullPrePopSelfEmployment: PrePopSelfEmployment = PrePopSelfEmployment(
    name = Some(name),
    trade = Some(trade),
    address = Some(address),
    startDate = Some(startDate),
    accountingMethod = accountingMethod
  )

  lazy val minimalPrePopSelfEmployment: PrePopSelfEmployment = PrePopSelfEmployment(
    name = None,
    trade = None,
    address = None,
    startDate = None,
    accountingMethod = accountingMethod
  )

  lazy val testUUID: String = "test-uuid"

  lazy val expectedFullSelfEmploymentData: SelfEmploymentData = SelfEmploymentData(
    id = testUUID,
    startDateBeforeLimit = Some(false),
    businessStartDate = Some(BusinessStartDate(startDate)),
    businessName = Some(BusinessNameModel(name)),
    businessTradeName = Some(BusinessTradeNameModel(trade)),
    businessAddress = Some(BusinessAddressModel(address))
  )

  lazy val expectedMinimalSelfEmploymentData: SelfEmploymentData = SelfEmploymentData(
    id = testUUID
  )

  lazy val expectedUkProperty: PropertyModel = PropertyModel(
    accountingMethod = Some(Accruals)
  )

  lazy val expectedForeignProperty: OverseasPropertyModel = OverseasPropertyModel(
    accountingMethod = Some(Cash)
  )
}
