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

package connectors.preferences

import connectors.mocks.TestPreferenceFrontendConnector
import org.scalatest.Matchers._
import play.api.test.FakeRequest
import utils.UnitTestTrait
import utils.TestConstants._

class PreferenceFrontendConnectorSpec extends UnitTestTrait
  with TestPreferenceFrontendConnector {

  implicit val fakeRequest = FakeRequest()

  "PreferenceFrontendConnector" should {

    "Provide the correct checkPaperless URL" in {
      TestPreferenceFrontendConnector.checkPaperlessUrl(testToken) should include regex """^.*\/paperless\/activate\?returnUrl=(.*)&returnLinkText=(.*)$"""
    }

    "Provide the correct choosePaperlessUrl URL" in {
      TestPreferenceFrontendConnector.choosePaperlessUrl should include regex """^.*\/paperless\/choose\?returnUrl=(.*)&returnLinkText=(.*)$"""
    }

  }

}
