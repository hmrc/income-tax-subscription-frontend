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

import connectors.mocks.MockGGAdminConnector
import connectors.models.gg.{KnownFactsFailure, KnownFactsResult, KnownFactsSuccess}
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestConstants._

import scala.concurrent.Future

class GGAdminConnectorSpec extends MockGGAdminConnector with ScalaFutures {

  override implicit val hc = HeaderCarrier()

  "GGAdminConnector.addKnownFacts" must {

    "Post to the correct url" in {
      TestGGAdminConnector.addKnownFactsUrl must endWith("/government-gateway-admin/service/HMRC-MTD-IT/known-facts")
    }

    def result: Future[KnownFactsResult] = TestGGAdminConnector.addKnownFacts(knownFactsRequest)

    "parse and return the success response correctly" in {
      mockAddKnownFactsSuccess(knownFactsRequest)
      whenReady(result)(_ mustBe KnownFactsSuccess)
    }

    "parse and return the Bad request response correctly" in {
      mockAddKnownFactsFailure(knownFactsRequest)
      whenReady(result)(_ mustBe KnownFactsFailure(errorJson.toString))
    }

  }

}
