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

import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccess
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.RemoveBusinessService
import services.RemoveBusinessService.RemoveBusinessFailure
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockRemoveBusinessService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockRemoveBusinessService: RemoveBusinessService = mock[RemoveBusinessService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRemoveBusinessService)
  }

  def verifyDeleteBusiness(businessId: String): Unit = {
    verify(mockRemoveBusinessService).deleteBusiness(ArgumentMatchers.any(), ArgumentMatchers.eq(businessId), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())
  }

  def mockDeleteBusiness(value: Future[Either[RemoveBusinessFailure, DeleteSubscriptionDetailsSuccess]]): Unit = {
    when(mockRemoveBusinessService.deleteBusiness
    (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any()))
      .thenReturn(value)
  }
}
