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

package connectors

import connectors.mocks.TestGGAuthenticationConnector
import connectors.models.authenticator.{RefreshProfileFailure, RefreshProfileResult, RefreshProfileSuccess}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class GGAuthenticationConnectorSpec extends TestGGAuthenticationConnector with ScalaFutures {

  override implicit val hc = HeaderCarrier()

  "GGAuthenticationConnector.refreshProfile" must {

    def refreshProfileResponse: Future[Either[RefreshProfileFailure.type, RefreshProfileSuccess.type]] = TestGGAuthenticationConnector.refreshProfile

    "return RefreshSuccessful when successful" in {
      mockRefreshProfileSuccess()
      whenReady(refreshProfileResponse)(_ mustBe Right(RefreshProfileSuccess))
    }

    "return RefreshSuccessful in case of failure" in {
      mockRefreshProfileFailure()
      whenReady(refreshProfileResponse)(_ mustBe Left(RefreshProfileFailure))
    }

    "pass through the exception when the connection fails" in {
      mockRefreshProfileException()
      whenReady(refreshProfileResponse.failed)(_ mustBe testException)
    }

  }

}
