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

package services.mocks

import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsResponse
import connectors.httpparser.RetrieveReferenceHttpParser.RetrieveReferenceResponse
import models.common._
import models.common.business.SelfEmploymentData
import models.{AccountingMethod, DateModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.SubscriptionDetailsService

import scala.concurrent.Future

//scalastyle:off

trait MockSubscriptionDetailsService extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockMandationStatusService with MockGetEligibilityStatusService {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionDetailsService)
  }

  val mockSubscriptionDetailsService: SubscriptionDetailsService = mock[SubscriptionDetailsService]

  def mockFetchReference(utr: String)(result: RetrieveReferenceResponse): Unit = {
    when(mockSubscriptionDetailsService.retrieveReference(ArgumentMatchers.eq(utr))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchSelectedTaxYear(result: Option[AccountingYearModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchSelectedTaxYear(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchEligibilityInterruptPassed(result: Option[Boolean]): Unit = {
    when(mockSubscriptionDetailsService.fetchEligibilityInterruptPassed(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchProperty(result: Option[PropertyModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchProperty(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchOverseasProperty(result: Option[OverseasPropertyModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchOverseasProperty(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchLastUpdatedTimestamp(result: Option[TimestampModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchLastUpdatedTimestamp(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockSaveSelectedTaxYear(accountingYearModel: AccountingYearModel)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveSelectedTaxYear(ArgumentMatchers.any(), ArgumentMatchers.eq(accountingYearModel))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }


  def mockSaveProperty(propertyModel: PropertyModel)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(propertyModel))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifySaveProperty(propertyModel: PropertyModel, count: Int = 1): Unit = {
    if (count == 0){
      verify(mockSubscriptionDetailsService, times(count)).saveProperty(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
    } else {
      verify(mockSubscriptionDetailsService, times(count)).saveProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(propertyModel))(ArgumentMatchers.any())
    }
  }

  def mockSaveOverseasProperty(overseasPropertyModel: OverseasPropertyModel)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveOverseasProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(overseasPropertyModel))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifySaveOverseasProperty(overseasPropertyModel: OverseasPropertyModel, count: Int = 1): Unit = {
    if (count == 0){
      verify(mockSubscriptionDetailsService, times(count)).saveOverseasProperty(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
    } else {
      verify(mockSubscriptionDetailsService, times(count)).saveOverseasProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(overseasPropertyModel))(ArgumentMatchers.any())
    }
  }

  def mockFetchIncomeSourceConfirmation(result: Option[Boolean]): Unit = {
    when(mockSubscriptionDetailsService.fetchIncomeSourcesConfirmation(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockSaveIncomeSourceConfirmation(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveIncomeSourcesConfirmation(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockSavePrePopFlag(flag: Boolean)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.savePrePopFlag(ArgumentMatchers.any(), ArgumentMatchers.eq(flag))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifySavePrePopFlag(flag: Boolean, count: Int = 1): Unit = {
    if(count == 0) {
      verify(mockSubscriptionDetailsService, times(count)).savePrePopFlag(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
    } else {
      verify(mockSubscriptionDetailsService, times(count)).savePrePopFlag(ArgumentMatchers.any(), ArgumentMatchers.eq(flag))(ArgumentMatchers.any())
    }
  }

  def mockFetchAllSelfEmployments(selfEmployments: Seq[SelfEmploymentData], accountingMethod: Option[AccountingMethod] = None): Unit = {
    when(mockSubscriptionDetailsService.fetchAllSelfEmployments(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful((selfEmployments, accountingMethod)))
  }

  def mockFetchPropertyStartDate(date: Option[DateModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchPropertyStartDate(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(date))
  }

  def mockSavePropertyStartDate(date: DateModel)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.savePropertyStartDate(ArgumentMatchers.any(), ArgumentMatchers.eq(date))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchPropertyAccountingMethod(accountingMethod: Option[AccountingMethod]): Unit = {
    when(mockSubscriptionDetailsService.fetchAccountingMethodProperty(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(accountingMethod))
  }

  def mockSavePropertyAccountingMethod(accountingMethod: AccountingMethod)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveAccountingMethodProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(accountingMethod))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchOverseasPropertyStartDate(date: Option[DateModel]): Unit = {
    when(mockSubscriptionDetailsService.fetchOverseasPropertyStartDate(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(date))
  }

  def mockSaveOverseasPropertyStartDate(date: DateModel)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveOverseasPropertyStartDate(ArgumentMatchers.any(), ArgumentMatchers.eq(date))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchOverseasPropertyAccountingMethod(accountingMethod: Option[AccountingMethod]): Unit = {
    when(mockSubscriptionDetailsService.fetchOverseasPropertyAccountingMethod(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(accountingMethod))
  }

  def mockSaveOverseasAccountingMethodProperty(accountingMethod: AccountingMethod)(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveOverseasAccountingMethodProperty(ArgumentMatchers.any(), ArgumentMatchers.eq(accountingMethod))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchAllIncomeSources(incomeSources: IncomeSources): Unit = {
    when(mockSubscriptionDetailsService.fetchAllIncomeSources(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(incomeSources))
  }

  def mockSaveBusinesses(businesses: Seq[SelfEmploymentData], accountingMethod: Option[AccountingMethod])(result: PostSubscriptionDetailsResponse): Unit = {
    when(mockSubscriptionDetailsService.saveBusinesses(ArgumentMatchers.any(), ArgumentMatchers.eq(businesses), ArgumentMatchers.eq(accountingMethod))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifySaveBusinesses(businesses: Seq[SelfEmploymentData], accountingMethod: Option[AccountingMethod], count: Int = 1): Unit = {
    if (count == 0) {
      verify(mockSubscriptionDetailsService, times(count)).saveBusinesses(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
    } else {
      verify(mockSubscriptionDetailsService, times(count)).saveBusinesses(ArgumentMatchers.any(), ArgumentMatchers.eq(businesses), ArgumentMatchers.eq(accountingMethod))(ArgumentMatchers.any())
    }
  }

  def mockFetchPrePopFlag(flag: Option[Boolean]): Unit = {
    when(mockSubscriptionDetailsService.fetchPrePopFlag(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(flag))
  }

  def verifyFetchPrePopFlag(): Unit = {
    verify(mockSubscriptionDetailsService).fetchPrePopFlag(ArgumentMatchers.any())(ArgumentMatchers.any())
  }

  def verifyFetchEligibilityInterruptPassed(): Unit = {
    verify(mockSubscriptionDetailsService).fetchEligibilityInterruptPassed(ArgumentMatchers.any())(ArgumentMatchers.any())
  }

}