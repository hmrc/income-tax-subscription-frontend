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

import connectors.MandationStatusConnector
import models.ErrorModel
import models.status.{MandationStatus, MandationStatusModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import utilities.UnitTestTrait

trait MockMandationStatusConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockMandationStatusConnector: MandationStatusConnector = mock[MandationStatusConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMandationStatusConnector)
  }

  def mockGetMandationStatus(nino: String, utr: String)(current: MandationStatus, next: MandationStatus): Unit = {
    when(mockMandationStatusConnector.getMandationStatus(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(utr))(ArgumentMatchers.any()))
      .thenReturn(Right(MandationStatusModel(current, next)))
  }

  def mockFailedGetMandationStatus(): Unit = {
    when(mockMandationStatusConnector.getMandationStatus(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Left(ErrorModel(INTERNAL_SERVER_ERROR, "Something went wrong")))
  }

}
