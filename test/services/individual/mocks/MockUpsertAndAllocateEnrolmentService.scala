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

package services.individual.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.individual.UpsertAndAllocateEnrolmentService
import services.individual.UpsertAndAllocateEnrolmentServiceModel.UpsertAndAllocateEnrolmentResponse

import scala.concurrent.Future

trait MockUpsertAndAllocateEnrolmentService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockUpsertAndAllocateEnrolmentService: UpsertAndAllocateEnrolmentService = mock[UpsertAndAllocateEnrolmentService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpsertAndAllocateEnrolmentService)
  }

  def mockUpsertAndAllocateEnrolment(mtditid: String, nino: String)(response: UpsertAndAllocateEnrolmentResponse): Unit =
    when(mockUpsertAndAllocateEnrolmentService.upsertAndAllocate(
      ArgumentMatchers.eq(mtditid),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

  def verifyUpsertAndAllocateEnrolment(mtditid: String, nino: String, count: Int = 1): Unit = {
    verify(mockUpsertAndAllocateEnrolmentService, times(count)).upsertAndAllocate(
      ArgumentMatchers.eq(mtditid),
      ArgumentMatchers.eq(nino)
    )(ArgumentMatchers.any())
  }

}
