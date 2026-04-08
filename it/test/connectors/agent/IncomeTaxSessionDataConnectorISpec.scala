/*
 * Copyright 2026 HM Revenue & Customs
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
import helpers.agent.servicemocks.IncomeTaxSessionDataStub.stubSetupViewAndChangeData
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HeaderCarrier

class IncomeTaxSessionDataConnectorISpec extends ComponentSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mtditid: String = "test-mtditid"
  val nino: String = "test-nino"
  val utr: String = "test-utr"

  "setupViewAndChangeSessionData" should {
    "return true" when {
      "a 2xx response was returned from the connector call" in {
        stubSetupViewAndChangeData(mtditid, nino, utr)(OK)

        connector.setupViewAndChangeSessionData(mtditid, nino, utr).futureValue mustBe true
      }
    }
    "return false" when {
      "a non 2xx response was returned from the connector call" in {
        stubSetupViewAndChangeData(mtditid, nino, utr)(INTERNAL_SERVER_ERROR)

        connector.setupViewAndChangeSessionData(mtditid, nino, utr).futureValue mustBe false
      }
    }
  }

  lazy val connector: IncomeTaxSessionDataConnector = app.injector.instanceOf[IncomeTaxSessionDataConnector]

}
