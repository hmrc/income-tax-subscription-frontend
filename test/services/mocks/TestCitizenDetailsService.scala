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

import connectors.usermatching.mocks.MockCitizenDetailsConnector
import models.usermatching.CitizenDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.individual.CitizenDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait TestCitizenDetailsService extends MockCitizenDetailsConnector {

  object TestCitizenDetailsService extends CitizenDetailsService(appConfig, mockCitizenDetailsConnector)

}

trait MockCitizenDetailsService extends UnitTestTrait with MockitoSugar {
  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]

  private def mockLookupUtr(nino: String)(response: Future[CitizenDetails]): Unit =
    when(
      mockCitizenDetailsService.lookupCitizenDetails(
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  def mockLookupUserWithUtr(nino: String)(utr: String, name: String): Unit =
    mockLookupUtr(nino)(Future.successful(CitizenDetails(Some(utr), Some(name))))

  def mockLookupUserWithoutUtr(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(CitizenDetails(None, None)))

  def mockLookupException(nino: String): Unit =
    mockLookupUtr(nino)(Future.failed(testException))

}
