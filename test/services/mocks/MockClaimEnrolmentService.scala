/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.individual.claimenrolment.ClaimEnrolmentService
import services.individual.claimenrolment.ClaimEnrolmentService.{ClaimEnrolmentFailure, ClaimEnrolmentResponse}

import scala.concurrent.Future

trait MockClaimEnrolmentService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(claimEnrolmentService)
  }

  val claimEnrolmentService: ClaimEnrolmentService = mock[ClaimEnrolmentService]

  def mockClaimEnrolment(response: ClaimEnrolmentResponse): Unit = {
    when(claimEnrolmentService.claimEnrolment(any(), any(), any())) thenReturn Future.successful(response)
  }

  def mockGetMtditidFromSubscription(response: Either[ClaimEnrolmentFailure, String]): Unit = {
    when(claimEnrolmentService.getMtditidFromSubscription(any(), any(), any())) thenReturn Future.successful(response)
  }

}
