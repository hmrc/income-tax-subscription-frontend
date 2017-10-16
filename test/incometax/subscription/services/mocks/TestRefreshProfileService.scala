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

package incometax.subscription.services.mocks

import incometax.subscription.connectors.mocks.MockGGAuthenticationConnector
import incometax.subscription.models.{RefreshProfileFailure, RefreshProfileSuccess}
import incometax.subscription.services.RefreshProfileService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockTrait
import utils.TestConstants._

import scala.concurrent.Future

trait TestRefreshProfileService extends MockGGAuthenticationConnector {

  object TestRefreshProfileService extends RefreshProfileService(mockGGAuthenticationConnector)

}

trait MockRefreshProfileService extends MockTrait {
  val mockRefreshProfileService = mock[RefreshProfileService]

  private def mockRefreshProfile(result: Future[Either[RefreshProfileFailure.type, RefreshProfileSuccess.type]]): Unit =
    when(mockRefreshProfileService.refreshProfile()(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def mockRefreshProfileSuccess(): Unit = {
    mockRefreshProfile(Future.successful(testRefreshProfileSuccess))
  }

  def mockRefreshProfileFailure(): Unit = {
    mockRefreshProfile(Future.successful(testRefreshProfileFailure))
  }

  def mockRefreshProfileException(): Unit = mockRefreshProfile(Future.failed(testException))
}
