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

import connectors.usermatching.mocks.MockCitizenDetailsConnector
import core.utils.MockTrait
import core.utils.TestConstants._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{AnyContent, Request}
import services.individual.{CitizenDetailsService, OptionalIdentifiers}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait TestCitizenDetailsService extends MockCitizenDetailsConnector {

  object TestCitizenDetailsService extends CitizenDetailsService(appConfig, mockCitizenDetailsConnector)

}

trait MockCitizenDetailsService extends MockTrait {
  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]

  private def mockLookupUtr(nino: String)(response: Future[Option[String]]) =
    when(
      mockCitizenDetailsService.lookupUtr(
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  private def mockLookupNino(utr: String)(response: Future[String]) =
    when(
      mockCitizenDetailsService.lookupNino(
        ArgumentMatchers.eq(utr)
      )(ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(response)

  def mockResolveIdentifiers(optNino: Option[String], optUtr: Option[String])
                            (optReturnNino: Option[String], optReturnUtr: Option[String]): Unit =
    when(
      mockCitizenDetailsService.resolveKnownFacts(
        ArgumentMatchers.eq(optNino),
        ArgumentMatchers.eq(optUtr)
      )(
        ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[Request[AnyContent]]
      )
    ).thenReturn(Future.successful(OptionalIdentifiers(optReturnNino, optReturnUtr)))

  def mockLookupUserWithUtr(nino: String)(utr: String): Unit =
    mockLookupUtr(nino)(Future.successful(Some(utr)))

  def mockLookupUserWithoutUtr(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(None))

  def mockLookupException(nino: String): Unit =
    mockLookupUtr(nino)(Future.failed(testException))

  def mockLookupNinoSuccess(utr: String): Unit =
    mockLookupNino(utr)(Future.successful(testNino))

}
