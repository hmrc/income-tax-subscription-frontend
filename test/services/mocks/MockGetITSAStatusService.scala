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

import models.status.{GetITSAStatus, GetITSAStatusModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.GetITSAStatusService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockGetITSAStatusService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockGetITSAStatusService: GetITSAStatusService = mock[GetITSAStatusService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetITSAStatusService)
  }

  def mockGetITSAStatusSuccess(nino: String)(status: GetITSAStatus): Unit = {
    when(mockGetITSAStatusService.getITSAStatus(ArgumentMatchers.any())(ArgumentMatchers.any[HeaderCarrier]()))
      .thenReturn(Future.successful(GetITSAStatusModel(status)))
  }

}
