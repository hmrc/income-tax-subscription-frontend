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

package services.mocks

import connectors.mocks.MockIdentityVerificationConnector
import connectors.models.iv.{IVFailure, IVJourneyResult, IVSuccess, IVTimeout}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.IdentityVerificationService
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.MockTrait

import scala.concurrent.Future

trait TestIdentityVerificationService extends MockIdentityVerificationConnector {

  object TestIdentityVerificationService extends IdentityVerificationService(mockIdentityVerificationConnector)

}


trait MockIdentityVerificationService extends MockTrait {

  val mockIdentityVerificationService: IdentityVerificationService = mock[IdentityVerificationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIdentityVerificationService)
  }

  private def mockJourneyResult(journeyId: String)(response: Future[IVJourneyResult]): Unit =
    when(mockIdentityVerificationService.getJourneyResult(ArgumentMatchers.contains(journeyId))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  val testException = new InternalServerException("future failed")

  def mockIVSuccess(journeyId: String): Unit = mockJourneyResult(journeyId)(IVSuccess)

  def mockIVTimeout(journeyId: String): Unit = mockJourneyResult(journeyId)(IVTimeout)

  def mockIVFailure(journeyId: String): Unit = mockJourneyResult(journeyId)(IVFailure)

  def mockIVException(journeyId: String): Unit =
    mockJourneyResult(journeyId)(Future.failed(testException))

}
