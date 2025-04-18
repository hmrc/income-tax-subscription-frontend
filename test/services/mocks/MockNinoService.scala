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
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.NinoService

import scala.concurrent.Future

trait MockNinoService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockNinoService: NinoService = mock[NinoService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNinoService)
  }

  def mockGetNino(nino: String): Unit = {
    when(mockNinoService.getNino(ArgumentMatchers.any())).thenReturn(
      Future.successful(nino)
    )
  }

  def verifyGetNino(): Unit = {
    verify(mockNinoService).getNino(ArgumentMatchers.any())
  }

}
