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
import services.PrePopDataService
import services.PrePopDataService.PrePopResult

import scala.concurrent.Future

trait MockPrePopDataService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockPrePopDataService: PrePopDataService = mock[PrePopDataService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPrePopDataService)
  }

  def mockPrePopIncomeSources(prePopResult: PrePopResult): Unit = {
    when(mockPrePopDataService.prePopIncomeSources(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(prePopResult))
  }

}
