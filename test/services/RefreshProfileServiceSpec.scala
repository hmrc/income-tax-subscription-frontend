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

import connectors.models.authenticator.{RefreshProfileFailure, RefreshProfileSuccess}
import org.scalatest.concurrent.ScalaFutures
import services.mocks.TestRefreshProfileService
import utils.UnitTestTrait

import scala.concurrent.Future

class RefreshProfileServiceSpec extends UnitTestTrait with TestRefreshProfileService with ScalaFutures {
  "refreshProfile" should {
    def result: Future[Either[RefreshProfileFailure.type, RefreshProfileSuccess.type]] = TestRefreshProfileService.refreshProfile()

    "return a success from the GGAuthenticationConnector" in {
      mockRefreshProfileSuccess()

      whenReady(result)(_ mustBe Right(RefreshProfileSuccess))
    }

    "return a failure from the GGAuthenticationConnector" in {
      mockRefreshProfileFailure()

      whenReady(result)(_ mustBe Left(RefreshProfileFailure))
    }

    "pass through the exception if the GGAuthenticationConnector fails" in {
      mockRefreshProfileException()

      whenReady(result.failed)(_ mustBe testException)
    }
  }
}
