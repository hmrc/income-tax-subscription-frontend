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

import config.MockConfig
import connectors.individual.eligibility.mocks.MockGetEligibilityStatusConnector
import models.EligibilityStatus
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.GetEligibilityStatusService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockGetEligibilityStatusService extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  val mockGetEligibilityStatusService: GetEligibilityStatusService = mock[GetEligibilityStatusService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetEligibilityStatusService)
  }

  def mockGetEligibilityStatus(result: EligibilityStatus): Unit = {
    when(mockGetEligibilityStatusService.getEligibilityStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }
}

trait TestGetEligibilityStatusService extends MockGetEligibilityStatusConnector with MockSessionDataService with MockNinoService with MockUTRService {
  suite: Suite =>

  object TestGetEligibilityStatusService extends GetEligibilityStatusService(
    mockGetEligibilityStatusConnector,
    mockNinoService,
    mockUTRService,
    mockSessionDataService
  )(MockConfig)

}
