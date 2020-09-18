/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.httpparser.PostSubscriptionDetailsHttpParser._
import models.common.BusinessNameModel
import models.individual.business.{BusinessAddressModel, BusinessStartDate, BusinessTradeNameModel, SelfEmploymentData}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.MultipleSelfEmploymentsService
import services.MultipleSelfEmploymentsService.SaveSelfEmploymentDataFailure

import scala.concurrent.Future

trait MockMultipleSelfEmploymentsService extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  val mockMultipleSelfEmploymentsService: MultipleSelfEmploymentsService = mock[MultipleSelfEmploymentsService]

  override def beforeEach(): Unit = {
    reset(mockMultipleSelfEmploymentsService)
    super.beforeEach()
  }

  def mockFetchBusinessStartDate(businessId: String)(response: Option[BusinessStartDate]): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchBusinessStartDate(ArgumentMatchers.eq(businessId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockFetchBusinessStartDateException(businessId: String): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchBusinessStartDate(ArgumentMatchers.eq(businessId))(ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Exception("Unexpected response: 500")))
  }

  def mockFetchBusinessName(businessId: String)(response: Option[BusinessNameModel]): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchBusinessName(ArgumentMatchers.eq(businessId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockFetchBusinessTrade(businessId: String)(response: Option[BusinessTradeNameModel]): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchBusinessTrade(ArgumentMatchers.eq(businessId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockFetchAllBusinesses(response: Seq[SelfEmploymentData]): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchAllBusinesses(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockFetchAllBusinessesException(): Unit = {
    when(mockMultipleSelfEmploymentsService.fetchAllBusinesses(ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Exception("Unexpected response: 500")))
  }

  def mockSaveBusinessStartDate(businessId: String, businessStartDate: BusinessStartDate)
                               (response: Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]): Unit = {
    when(
      mockMultipleSelfEmploymentsService.saveBusinessStartDate(
        ArgumentMatchers.eq(businessId),
        ArgumentMatchers.eq(businessStartDate)
      )(ArgumentMatchers.any())
    ).thenReturn(Future.successful(response))
  }

  def mockSaveBusinessName(businessId: String, businessName: BusinessNameModel)
                          (response: Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]): Unit = {
    when(
      mockMultipleSelfEmploymentsService.saveBusinessName(
        ArgumentMatchers.eq(businessId),
        ArgumentMatchers.eq(businessName)
      )(ArgumentMatchers.any())
    ).thenReturn(Future.successful(response))
  }

  def mockSaveBusinessTrade(businessId: String, businessTrade: BusinessTradeNameModel)
                           (response: Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]): Unit = {
    when(
      mockMultipleSelfEmploymentsService.saveBusinessTrade(
        ArgumentMatchers.eq(businessId),
        ArgumentMatchers.eq(businessTrade)
      )(ArgumentMatchers.any())
    ).thenReturn(Future.successful(response))
  }

  def mockSaveBusinessAddress(businessId: String, businessAddress: BusinessAddressModel)
                          (response: Either[SaveSelfEmploymentDataFailure.type, PostSubscriptionDetailsSuccess]): Unit = {
    when(
      mockMultipleSelfEmploymentsService.saveBusinessAddress(
        ArgumentMatchers.eq(businessId),
        ArgumentMatchers.eq(businessAddress)
      )(ArgumentMatchers.any())
    ).thenReturn(Future.successful(response))
  }
}
