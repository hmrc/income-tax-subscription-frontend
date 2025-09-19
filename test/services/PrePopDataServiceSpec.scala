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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import connectors.mocks.MockPrePopConnector
import models._
import models.common.business._
import models.prepop.{PrePopData, PrePopSelfEmployment}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PrePopDataService.PrePopResult.{PrePopFailure, PrePopSuccess}
import services.mocks.MockSubscriptionDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.testNino
import utilities.{AccountingPeriodUtil, MockUUIDProvider}

import scala.concurrent.ExecutionContext.Implicits.global

class PrePopDataServiceSpec extends PlaySpec
  with Matchers
  with MockPrePopConnector
  with MockSubscriptionDetailsService
  with MockUUIDProvider {

  val appConfig: AppConfig = MockConfig

  val service: PrePopDataService = new PrePopDataService(
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
              businesses = Seq(expectedFullSelfEmploymentData, expectedFullSelfEmploymentData)
            )(Right(PostSubscriptionDetailsSuccessResponse))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true)
            verifySaveBusinesses(
              businesses = Seq(expectedFullSelfEmploymentData, expectedFullSelfEmploymentData)
            )
          }
          "the fetched pre-pop data has minimal income source data and saving of the data was successful" in {
            mockFetchPrePopFlag(None)
            mockGetPrePopData(testNino)(Right(minimalPrePopData))
            mockUUID(testUUID)
            mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
            mockSaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData)
            )(Right(PostSubscriptionDetailsSuccessResponse))

            await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

            verifyFetchPrePopFlag()
            verifyGetPrePopData(testNino)
            verifySavePrePopFlag(flag = true)
            verifySaveBusinesses(
              businesses = Seq(expectedMinimalSelfEmploymentData)
            )
          }
          "the fetched pre-pop data has only self employment data" which {
            "was saved successfully" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(selfEmploymentOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData)
              )(Right(PostSubscriptionDetailsSuccessResponse))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopSuccess

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData)
              )
            }
            "returned a save failure when saving" in {
              mockFetchPrePopFlag(None)
              mockGetPrePopData(testNino)(Right(selfEmploymentOnlyPrePopData))
              mockUUID(testUUID)
              mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))
              mockSaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData)
              )(Left(PostSubscriptionDetailsHttpParser.UnexpectedStatusFailure(INTERNAL_SERVER_ERROR)))

              await(service.prePopIncomeSources(reference, testNino)) mustBe PrePopFailure(UnexpectedStatusFailure(INTERNAL_SERVER_ERROR).toString)

              verifyFetchPrePopFlag()
              verifyGetPrePopData(testNino)
              verifySavePrePopFlag(flag = true)
              verifySaveBusinesses(
                businesses = Seq(expectedMinimalSelfEmploymentData)
              )
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
              count = 0
            )
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
              count = 0
            )
          }
        }
      }
    }
  }

  lazy val fullPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      fullPrePopSelfEmployment,
      fullPrePopSelfEmployment
    ))
  )

  lazy val minimalPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      minimalPrePopSelfEmployment
    ))
  )

  lazy val selfEmploymentOnlyPrePopData: PrePopData = PrePopData(
    selfEmployment = Some(Seq(
      minimalPrePopSelfEmployment
    ))
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

  lazy val fullPrePopSelfEmployment: PrePopSelfEmployment = PrePopSelfEmployment(
    name = Some(name),
    trade = Some(trade),
    address = Some(address),
    startDate = Some(startDate)
  )

  lazy val minimalPrePopSelfEmployment: PrePopSelfEmployment = PrePopSelfEmployment(
    name = None,
    trade = None,
    address = None,
    startDate = None
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
}
