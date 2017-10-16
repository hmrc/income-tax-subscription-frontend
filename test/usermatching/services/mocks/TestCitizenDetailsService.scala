/*
 * Copyright 2017 HM Revenue & Customs
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

package usermatching.services.mocks

import connectors.models.{CitizenDetailsFailureResponse, CitizenDetailsSuccess}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import usermatching.connectors.mocks.MockCitizenDetailsConnector
import usermatching.services.CitizenDetailsService
import utils.MockTrait
import utils.TestConstants._

import scala.concurrent.Future

trait TestCitizenDetailsService extends MockCitizenDetailsConnector {

  object TestCitizenDetailsService extends CitizenDetailsService(appConfig, mockCitizenDetailsConnector)

}

trait MockCitizenDetailsService extends MockTrait {
  val mockCitizenDetailsService: CitizenDetailsService = mock[CitizenDetailsService]

  private def mockLookupUtr(nino: String)(response: Future[Either[CitizenDetailsFailureResponse, Option[CitizenDetailsSuccess]]]) =
    when(
      mockCitizenDetailsService.lookupUtr(
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  def mockLookupUserWithUtr(nino: String)(utr: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetailsSuccess(Some(utr))))))

  def mockLookupUserWithoutUtr(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetailsSuccess(None)))))

  def mockLookupUserNotFound(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(None)))

  def mockLookupFailure(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Left(CitizenDetailsFailureResponse(BAD_REQUEST))))

  def mockLookupException(nino: String): Unit =
    mockLookupUtr(nino)(Future.failed(testException))

}
