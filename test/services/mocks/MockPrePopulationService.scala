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
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.PrePopulationService
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockPrePopulationService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockPrePopulationService: PrePopulationService = mock[PrePopulationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPrePopulationService)
  }

  def setupMockPrePopulateSave(reference: String): Unit = {
    when(
      mockPrePopulationService.prePopulate
      (ArgumentMatchers.eq(reference), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    )
      .thenReturn(Future.successful(()))
  }

  protected final def verifyPrePopulationSave(count: Int, reference: String): Unit =
    verify(mockPrePopulationService, times(count)).prePopulate(
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.any(),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

}

trait TestPrePopulationService extends MockSubscriptionDetailsService {

  object TestPrePopulationService extends PrePopulationService(MockSubscriptionDetailsService)

}
