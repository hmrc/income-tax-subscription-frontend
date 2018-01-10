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

package incometax.unauthorisedagent.connectors.mocks

import core.utils.MockTrait
import incometax.unauthorisedagent.connectors.AgentServicesAccountConnector
import incometax.unauthorisedagent.httpparsers.GetAgencyNameResponseHttpParser.GetAgencyNameResponse
import incometax.unauthorisedagent.models.{GetAgencyNameFailure, GetAgencyNameSuccess}
import core.utils.TestConstants._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockAgentServicesAccountConnector extends MockTrait {
  val mockAgentServicesAccountConnector = mock[AgentServicesAccountConnector]

  def mockGetAgencyName(arn: String)(result: Future[GetAgencyNameResponse]): Unit =
    when(mockAgentServicesAccountConnector.getAgencyName(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier])) thenReturn result

  val getAgencyNameSuccess = Future.successful(Right(GetAgencyNameSuccess(testAgencyName)))

  val getAgencyNameFailure = Future.successful(Left(GetAgencyNameFailure(testErrorMessage)))
}
