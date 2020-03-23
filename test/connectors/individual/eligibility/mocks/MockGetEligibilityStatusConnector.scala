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

package connectors.individual.eligibility.mocks

import auth.MockHttp
import config.AppConfig
import connectors.individual.eligibility.GetEligibilityStatusConnector
import connectors.individual.eligibility.httpparsers.EligibilityStatus
import utilities.HttpResult.HttpResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsValue
import utilities.MockTrait

import scala.concurrent.Future

trait MockGetEligibilityStatusConnector extends MockTrait {

  val mockGetEligibilityStatusConnector: GetEligibilityStatusConnector = mock[GetEligibilityStatusConnector]

  def mockGetEligibilityStatus(sautr: String)(result: Future[HttpResult[EligibilityStatus]]): Unit =
    when(mockGetEligibilityStatusConnector.getEligibilityStatus(ArgumentMatchers.eq(sautr))(ArgumentMatchers.any())).thenReturn(result)

}

trait TestGetEligibilityStatusConnector extends MockHttp {

  object TestGetEligibilityStatusConnector extends GetEligibilityStatusConnector(
    app.injector.instanceOf[AppConfig],
    mockHttp
  )

  def setupMockGetEligibilityStatus(sautr: String)(status: Int, response: JsValue): Unit =
    setupMockHttpGet(url = Some(TestGetEligibilityStatusConnector.eligibilityUrl(sautr)))(status, response)
}
