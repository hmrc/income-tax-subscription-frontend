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

import connectors.individual.eligibility.httpparsers.EligibilityStatus
import connectors.individual.eligibility.mocks.MockGetEligibilityStatusConnector
import core.utils.HttpResult.HttpResult
import core.utils.MockTrait
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.GetEligibilityStatusService

import scala.concurrent.Future

trait MockGetEligibilityStatusService extends MockTrait {
  val mockGetEligibilityStatusService = mock[GetEligibilityStatusService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetEligibilityStatusService)
  }

  def mockGetEligibilityStatus(sautr: String)(result: Future[HttpResult[EligibilityStatus]]): Unit =
    when(mockGetEligibilityStatusService.getEligibilityStatus(ArgumentMatchers.eq(sautr))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(result)

}

trait TestGetEligibilityStatusService extends MockGetEligibilityStatusConnector {

  object TestGetEligibilityStatusService extends GetEligibilityStatusService(mockGetEligibilityStatusConnector)

}
