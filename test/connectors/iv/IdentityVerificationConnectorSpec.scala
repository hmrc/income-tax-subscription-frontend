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

package connectors.iv

import connectors.mocks.TestIdentityVerificationConnector
import connectors.models.iv.{IVFailure, IVSuccess, IVTimeout}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import uk.gov.hmrc.play.http.InternalServerException


class IdentityVerificationConnectorSpec extends TestIdentityVerificationConnector
  with ScalaFutures with IntegrationPatience {

  val testJourneyId = "testId"

  "IdentityVerificationConnector.getJourneyResult" should {

    "return IVSuccess when it's successful" in {
      mockIVSuccess(testJourneyId)

      val result = TestIdentityVerificationConnector.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVSuccess)
    }

    "return IVTimeout when it's time out" in {
      mockIVTimeout(testJourneyId)

      val result = TestIdentityVerificationConnector.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVTimeout)
    }

    "return IVFailure when it's not a success or a time out" in {
      mockIVFailure(testJourneyId)

      val result = TestIdentityVerificationConnector.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVFailure)
    }

    "return IVFailure when it's the journey id resulted in a not found" in {
      mockIVNotFound(testJourneyId)

      val result = TestIdentityVerificationConnector.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVFailure)
    }

    "return failed future when an unexpected status code is returned" in {
      mockIVException(testJourneyId)

      val result = TestIdentityVerificationConnector.getJourneyResult(testJourneyId)
      whenReady(result.failed)(_.isInstanceOf[InternalServerException] mustBe true)
    }

  }

}