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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.agent.AutoEnrolmentService
import services.agent.AutoEnrolmentService.AutoClaimEnrolmentResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockAutoEnrolmentService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAutoEnrolmentService)
  }

  val mockAutoEnrolmentService: AutoEnrolmentService = mock[AutoEnrolmentService]

  def mockAutoClaimEnrolment(utr: String, nino: String, mtditid: String)(response: AutoClaimEnrolmentResponse): Unit = {
    when(mockAutoEnrolmentService.autoClaimEnrolment(
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(mtditid)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn Future.successful(response)
  }

}
