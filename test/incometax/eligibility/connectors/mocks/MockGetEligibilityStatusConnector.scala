/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.eligibility.connectors.mocks

import core.config.AppConfig
import core.connectors.mocks.MockHttp
import core.utils.HttpResult.HttpResult
import core.utils.MockTrait
import incometax.eligibility.connectors.GetEligibilityStatusConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait MockGetEligibilityStatusConnector extends MockTrait {

  val mockGetEligibilityStatusConnector = mock[GetEligibilityStatusConnector]

  def mockGetEligibilityStatus(sautr: String)(result: Future[HttpResult[Boolean]]): Unit =
    when(mockGetEligibilityStatusConnector.getEligibilityStatus(ArgumentMatchers.eq(sautr))).thenReturn(result)

}

trait TestGetEligibilityStatusConnector extends MockHttp {

  object TestGetEligibilityStatusConnector extends GetEligibilityStatusConnector(
    app.injector.instanceOf[AppConfig],
    mockHttp
  )

  def setupMockGetEligibilityStatus(sautr: String)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(url = Some(TestGetEligibilityStatusConnector.eligibilityUrl(sautr)))(status, response)
}