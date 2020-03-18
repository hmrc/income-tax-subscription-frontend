/*
 * Copyright 2020 HM Revenue & Customs
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

import models.audits.ClientMatchingAuditing._
import agent.utils.TestConstants._
import agent.utils.TestModels._
import core.utils.MockTrait
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.AuditingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditingServiceSpec extends MockTrait with BeforeAndAfterEach {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val testAuditingService = new AuditingService(appConfig, mockAuditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector)
  }

  "audit" when {
    "given a ClientMatchingAuditModel of type ClientMatchingRequest" should {
      "extract the data and pass it into the AuditConnector" in {

        val path: String = "/test-path"

        implicit val request: Request[_] = FakeRequest("GET", path)

        val testModel = ClientMatchingAuditModel(testARN, testClientDetails, isSuccess = true)
        val expectedData = AuditingService.toDataEvent(appConfig.appName, testModel, path)

        testAuditingService.audit(testModel)

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
