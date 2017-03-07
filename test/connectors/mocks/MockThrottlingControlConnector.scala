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

package connectors.mocks

import audit.Logging
import connectors.throttling.ThrottlingControlConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify}
import uk.gov.hmrc.play.http.HttpResponse
import utils.UnitTestTrait

trait MockThrottlingControlConnector extends UnitTestTrait with MockHttp {

  object TestThrottlingControlConnector extends
    ThrottlingControlConnector(appConfig, mockHttpGet, app.injector.instanceOf[Logging])

  def setupMockCheckAccess(nino: String)(status: Int): Unit =
    setupMockHttpGet(s"${appConfig.throttleControlUrl}/$nino")(status, None)

  def verifyMockCheckAccess(checkAccess: Int): Unit = {
    verify(mockHttpGet, times(checkAccess)).GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
  }
}
