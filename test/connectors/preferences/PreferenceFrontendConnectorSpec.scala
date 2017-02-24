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

import config.ITSAHeaderCarrierForPartialsConverter._
import connectors.mocks.MockPreferenceFrontendConnector
import connectors.models.preferences._
import org.scalatest.Matchers._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.UnitTestTrait

class PreferenceFrontendConnectorSpec extends UnitTestTrait
  with MockPreferenceFrontendConnector {

  implicit val fakeRequest = FakeRequest()

  "PreferenceFrontendConnector" should {

    "Provide the correct checkPaperless URL" in {
      TestPreferenceFrontendConnector.checkPaperlessUrl should include regex """^.*\/paperless\/activate\?returnUrl=(.*)&returnLinkText=(.*)$"""
    }

    "Provide the correct choosePaperlessUrl URL" in {
      TestPreferenceFrontendConnector.choosePaperlessUrl should include regex """^.*\/paperless\/choose\?returnUrl=(.*)&returnLinkText=(.*)$"""
    }

  }

  "PreferenceFrontendConnector.checkPaperless" should {

    "return Activated if checkPaperless returns a 200 and indicated activation is true" in {
      val expected = Activated

      setupCheckPaperless(paperlessActivated)
      val actual = TestPreferenceFrontendConnector.checkPaperless

      await(actual) shouldBe expected
    }

    "return Declined if checkPaperless returns a 200 and indicated activation is false" in {
      val expected = Declined

      setupCheckPaperless(paperlessDeclined)
      val actual = TestPreferenceFrontendConnector.checkPaperless

      await(actual) shouldBe expected
    }

    "return Unset if checkPaperless returns a 412" in {
      val expected = Unset

      setupCheckPaperless(paperlessPreconditionFailed)
      val actual = TestPreferenceFrontendConnector.checkPaperless

      await(actual) shouldBe expected
    }

  }

}
