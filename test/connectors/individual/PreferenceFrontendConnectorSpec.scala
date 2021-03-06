/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.individual

import connectors.individual.mocks.TestPreferenceFrontendConnector
import utilities.individual.TestConstants._
import org.scalatest.Matchers._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utilities.UnitTestTrait

class PreferenceFrontendConnectorSpec extends UnitTestTrait
  with TestPreferenceFrontendConnector {

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "PreferenceFrontendConnector" should {

    "Provide the correct checkPaperless URL" in {
      val urlPattern = """^.*\/paperless\/activate-from-token\/mtdfbit\/(.*)\?returnUrl=(.*)&returnLinkText=(.*)$"""

      TestPreferenceFrontendConnector.checkPaperlessUrl(testToken) should include regex urlPattern

    }

    "Provide the correct choosePaperlessUrl URL" in {
      TestPreferenceFrontendConnector.choosePaperlessUrl should include regex """^.*\/paperless\/choose\?returnUrl=(.*)&returnLinkText=(.*)$"""
    }

  }

}
