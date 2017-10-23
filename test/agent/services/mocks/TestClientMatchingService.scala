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

package agent.services.mocks

import agent.connectors.mocks.{MockAuthenticatorConnector, TestAuthenticatorConnector}
import agent.models.agent.ClientDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import agent.services.ClientMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import agent.utils.MockTrait
import agent.utils.TestConstants._

import scala.concurrent.Future

trait TestClientMatchingService extends MockAuthenticatorConnector {
  object TestClientMatchingService extends ClientMatchingService(appConfig, mockAuthenticatorConnector)
}

trait MockClientMatchingService extends MockTrait {
  val mockClientMatchingService = mock[ClientMatchingService]

  private def mockClientMatch(clientDetails: ClientDetailsModel)
                             (response: Future[Option[String]]): Unit =
    when(
      mockClientMatchingService.matchClient(
        ArgumentMatchers.eq(clientDetails)
      )(
        ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(response)

  def mockClientMatchSuccess(clientDetails: ClientDetailsModel): Unit = {
    mockClientMatch(clientDetails)(Future.successful(Some(testNino)))
  }

  def mockClientMatchNotFound(clientDetails: ClientDetailsModel): Unit = {
    mockClientMatch(clientDetails)(Future.successful(None))
  }

  def mockClientMatchException(clientDetails: ClientDetailsModel): Unit =
    mockClientMatch(clientDetails)(Future.failed(testException))
}
