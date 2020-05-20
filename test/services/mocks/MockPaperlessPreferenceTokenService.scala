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

import connectors.individual.mocks.MockPaperlessPreferenceTokenConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import services.PaperlessPreferenceTokenService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait MockPaperlessPreferenceTokenService extends UnitTestTrait with MockitoSugar {
  val mockPaperlessPreferenceTokenService = mock[PaperlessPreferenceTokenService]

  private def mockStoreNino(nino: String)(result: Future[String]) =
    when(mockPaperlessPreferenceTokenService.storeNino(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockStoreNinoSuccess(nino: String): Unit = mockStoreNino(nino)(Future.successful(testToken))

  def verifyStoreNino(nino: String): Unit = verify(mockPaperlessPreferenceTokenService)
    .storeNino(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier])
}

trait TestPaperlessPreferenceTokenService extends MockPaperlessPreferenceTokenConnector with MockKeystoreService {
  object TestPaperlessPreferenceTokenService extends PaperlessPreferenceTokenService(MockKeystoreService, mockPaperlessPreferenceTokenConnector)
}
