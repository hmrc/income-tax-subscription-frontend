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

package connectors.usermatching.mocks

import connectors.usermatching.CitizenDetailsConnector
import models.usermatching.{CitizenDetails, CitizenDetailsFailureResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utilities.UnitTestTrait
import utilities.individual.TestConstants.testException

import scala.concurrent.Future


trait MockCitizenDetailsConnector extends UnitTestTrait with MockitoSugar {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]

  private def mockLookupUtr(nino: String)(response: Future[Either[CitizenDetailsFailureResponse, Option[CitizenDetails]]]): Unit =
    when(
      mockCitizenDetailsConnector.lookupCitizenDetails(
        ArgumentMatchers.eq(nino)
      )(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ).thenReturn(response)

  def mockLookupUserWithUtr(nino: String)(utr: String, name: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetails(Some(utr), Some(name))))))

  def mockLookupUserWithoutUtr(nino: String): Unit =
    mockLookupUtr(nino)(Future.successful(Right(Some(CitizenDetails(None, None)))))

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
    app.injector.instanceOf[HttpClientV2]
  )

}
