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

package connectors.individual.mocks

import connectors.PaperlessPreferenceTokenConnector
import models.PaperlessPreferenceTokenResult.{PaperlessPreferenceTokenResult, PaperlessPreferenceTokenSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockPaperlessPreferenceTokenConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {
  val mockPaperlessPreferenceTokenConnector: PaperlessPreferenceTokenConnector = mock[PaperlessPreferenceTokenConnector]

  private def mockStoreNino(nino: String)(result: Future[PaperlessPreferenceTokenResult]) =
    when(mockPaperlessPreferenceTokenConnector.storeNino(ArgumentMatchers.any[String], ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)


  def mockStoreNinoSuccess(nino: String): Unit = mockStoreNino(nino)(Future.successful(Right(PaperlessPreferenceTokenSuccess)))

  def verifyStoreNino(nino: String): Unit = verify(mockPaperlessPreferenceTokenConnector)
    .storeNino(ArgumentMatchers.any[String], ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier])
}
