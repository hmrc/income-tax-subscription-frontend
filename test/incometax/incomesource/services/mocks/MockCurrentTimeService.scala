/*
 * Copyright 2018 HM Revenue & Customs
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

package incometax.incomesource.services.mocks

import core.utils.MockTrait
import incometax.incomesource.services.CurrentTimeService
import org.mockito.Mockito.{reset, when}

trait MockCurrentTimeService extends MockTrait {
  val mockCurrentTimeService = mock[CurrentTimeService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCurrentTimeService)
  }

  def mockGetTaxYearEnd(taxYearEnd: Int): Unit =
    when(mockCurrentTimeService.getTaxYearEndForCurrentDate).thenReturn(taxYearEnd)

}