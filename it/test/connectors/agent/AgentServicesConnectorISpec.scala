/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.agent

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AgentServicesStub
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

class AgentServicesConnectorISpec extends ComponentSpecBase {

  "isPreExistingRelationship" when {
    "an OK status response is returned from the API" must {
      "return true" in {
        AgentServicesStub.stubClientRelationship(arn, nino)(
          status = OK
        )

        val result = connector.isPreExistingRelationship(arn, nino)

        await(result) mustBe true
      }
    }
    "a NOT_FOUND status response is returned from the API" must {
      "return false" in {
        AgentServicesStub.stubClientRelationship(arn, nino)(
          status = NOT_FOUND
        )

        val result = connector.isPreExistingRelationship(arn, nino)

        await(result) mustBe false
      }
    }
    "an unsupported status response is returned from the API" must {
      "throw an internal server exception" in {
        AgentServicesStub.stubClientRelationship(arn, nino)(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.isPreExistingRelationship(arn, nino)

        intercept[InternalServerException](await(result))
          .message mustBe s"[AgentServicesConnector][isPreExistingRelationship] failure, status: $INTERNAL_SERVER_ERROR body="
      }
    }
  }

  "isMTDPreExistingRelationship" when {
    "an OK status response is returned from the API" must {
      "return true" in {
        AgentServicesStub.stubMTDClientRelationship(arn, nino)(
          status = OK
        )

        val result = connector.isMTDPreExistingRelationship(arn, nino)

        await(result) mustBe Right(true)
      }
    }
    "a NOT_FOUND status response is returned from the API" must {
      "return false" in {
        AgentServicesStub.stubMTDClientRelationship(arn, nino)(
          status = NOT_FOUND
        )

        val result = connector.isMTDPreExistingRelationship(arn, nino)

        await(result) mustBe Right(false)
      }
    }
    "an unsupported status response is returned from the API" must {
      "return an unexpected status failure response" in {
        AgentServicesStub.stubMTDClientRelationship(arn, nino)(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.isMTDPreExistingRelationship(arn, nino)

        await(result) mustBe Left(AgentServicesConnector.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "isMTDSuppAgentRelationship" when {
    "an OK status response is returned from the API" must {
      "return true" in {
        AgentServicesStub.stubMTDSuppAgentRelationship(arn, nino)(
          status = OK
        )

        val result = connector.isMTDSuppAgentRelationship(arn, nino)

        await(result) mustBe Right(true)
      }
    }
    "an unsupported status response is returned from the API" must {
      "return an unexpected status failure response" in {
        AgentServicesStub.stubMTDSuppAgentRelationship(arn, nino)(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.isMTDSuppAgentRelationship(arn, nino)

        await(result) mustBe Left(AgentServicesConnector.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

  lazy val connector: AgentServicesConnector = app.injector.instanceOf[AgentServicesConnector]

  lazy val arn: String = "test-arn"
  lazy val nino: String = "test-nino"

  implicit val hc: HeaderCarrier = HeaderCarrier()

}
