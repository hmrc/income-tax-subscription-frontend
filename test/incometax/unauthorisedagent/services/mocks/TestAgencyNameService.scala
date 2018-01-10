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

package incometax.unauthorisedagent.services.mocks

import core.utils.MockTrait
import incometax.unauthorisedagent.connectors.mocks.MockAgentServicesAccountConnector
import incometax.unauthorisedagent.services.AgencyNameService
import org.mockito.ArgumentMatchers
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Mockito._
import core.utils.TestConstants._

import scala.concurrent.Future

trait MockAgencyNameService extends MockTrait {
  val mockAgencyNameService = mock[AgencyNameService]

  override def beforeEach(): Unit ={
    super.beforeEach()
    reset(mockAgencyNameService)
  }

  def mockGetAgencyName(arn: String)(result: Future[String]): Unit =
    when(mockAgencyNameService.getAgencyName(ArgumentMatchers.eq(arn))(ArgumentMatchers.any[HeaderCarrier])) thenReturn result

  def mockGetAgencyNameSuccess(arn: String): Unit = mockGetAgencyName(arn)(testAgencyName)

  def mockGetAgencyNameFailure(arn: String): Unit = Future.failed(testException)
}

trait TestAgencyNameService extends MockAgentServicesAccountConnector {

  object TestAgencyNameService extends AgencyNameService(
    mockAgentServicesAccountConnector
  )

}
