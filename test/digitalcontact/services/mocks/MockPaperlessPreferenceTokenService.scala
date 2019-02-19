/*
 * Copyright 2019 HM Revenue & Customs
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

package digitalcontact.services.mocks

import core.services.mocks.MockKeystoreService
import digitalcontact.connectors.mocks.MockPaperlessPreferenceTokenConnector
import digitalcontact.services.PaperlessPreferenceTokenService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.MockTrait
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockPaperlessPreferenceTokenService extends MockTrait {
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
