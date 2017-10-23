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

package agent.audit

import agent.audit.AuditingService._
import agent.audit.models.ClientMatchingAuditing._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Configuration
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import agent.utils.MockTrait
import agent.utils.TestConstants._
import agent.utils.TestModels._

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier

class AuditingServiceSpec extends MockTrait with BeforeAndAfterEach {
  val mockAuditConnector = mock[AuditConnector]
  val mockConfiguration = mock[Configuration]
  val testAppName = "app"

  val testAuditingService = new AuditingService(mockConfiguration, mockAuditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockConfiguration)

    when(mockConfiguration.getString("appName")) thenReturn Some(testAppName)
  }

  "audit" when {
    "given a ClientMatchingAuditModel of type ClientMatchingRequest" should {
      "extract the data and pass it into the AuditConnector" in {
        val testModel = ClientMatchingAuditModel(testARN, testClientDetails, isSuccess = true)

        val expectedData = toDataEvent(testAppName, testModel, agent.controllers.matching.routes.ConfirmClientController.submit().url)

        testAuditingService.audit(testModel, agent.controllers.matching.routes.ConfirmClientController.submit().url)

        verify(mockAuditConnector)
          .sendEvent(
            ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
          )(
            ArgumentMatchers.any[HeaderCarrier],
            ArgumentMatchers.any[ExecutionContext]
          )
      }
    }
  }
}
