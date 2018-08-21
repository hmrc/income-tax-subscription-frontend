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

package usermatching.connectors.mocks

import core.audit.Logging
import core.utils.TestConstants.testException
import core.utils.{MockTrait, UnitTestTrait}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import usermatching.connectors.CitizenDetailsConnector
import usermatching.models.{CitizenDetailsFailureResponse, CitizenDetailsSuccess}

import scala.concurrent.Future


trait MockCitizenDetailsConnector extends MockTrait {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]

  private def mockLookupUtr(nino: String)(response: Future[Either[CitizenDetailsFailureResponse, Option[CitizenDetailsSuccess]]]) =
    when(
      mockCitizenDetailsConnector.lookupUtr(
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  def mockLookupNino(utr: String)(response: Future[Either[CitizenDetailsFailureResponse, Option[CitizenDetailsSuccess]]]): Unit =
    when(
      mockCitizenDetailsConnector.lookupNino(
        ArgumentMatchers.eq(utr)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  def mockLookupUserWithUtr(nino: String)(utr: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetailsSuccess(Some(utr), nino)))))

  def mockLookupUserWithoutUtr(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetailsSuccess(None, nino)))))

  def mockLookupUserNotFound(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(None)))

  def mockLookupFailure(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Left(CitizenDetailsFailureResponse(BAD_REQUEST))))

  def mockLookupException(nino: String): Unit =
    mockLookupUtr(nino)(Future.failed(testException))

}

trait TestCitizenDetailsConnector extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  object TestCitizenDetailsConnector extends CitizenDetailsConnector(
    appConfig,
    app.injector.instanceOf[HttpClient],
    app.injector.instanceOf[Logging]
  )

}