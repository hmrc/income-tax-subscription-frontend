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

package services

import connectors.models.iv.{IVFailure, IVSuccess, IVTimeout}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import services.mocks.TestIdentityVerificationService
import uk.gov.hmrc.play.http.InternalServerException


class IdentityVerificationServiceSpec extends TestIdentityVerificationService
  with ScalaFutures with IntegrationPatience {

  val testJourneyId = "testId"

  "IdentityVerificationService.getJourneyResult" should {

    "return IVSuccess if the connector returns IVSuccess" in {
      mockIVSuccess(testJourneyId)

      val result = TestIdentityVerificationService.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVSuccess)
    }

    "return IVTimeout if the connector returns IVTimeout" in {
      mockIVTimeout(testJourneyId)

      val result = TestIdentityVerificationService.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVTimeout)
    }

    "return IVFailure if the connector returns IVFailure" in {
      mockIVFailure(testJourneyId)

      val result = TestIdentityVerificationService.getJourneyResult(testJourneyId)
      whenReady(result)(_ mustBe IVFailure)
    }

    "return failed future if the connector returns failed future" in {
      mockIVException(testJourneyId)

      val result = TestIdentityVerificationService.getJourneyResult(testJourneyId)
      whenReady(result.failed)(_.isInstanceOf[InternalServerException] mustBe true)
    }

  }

}
