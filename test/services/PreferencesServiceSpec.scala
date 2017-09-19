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

package services

import connectors.models.preferences.{Activated, Unset}
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.TestPreferencesService
import utils.TestConstants._
import utils.UnitTestTrait

class PreferencesServiceSpec extends UnitTestTrait with TestPreferencesService with EitherValues {

  implicit val fakeRequest = FakeRequest()

  "TestPreferencesService" should {

    "Provide the correct choosePaperlessUrl URL" in {
      mockChoosePaperlessUrl(testUrl)

      TestPreferencesService.defaultChoosePaperlessUrl shouldBe testUrl
    }

  }

  "TestPreferencesService.checkPaperless" should {

    "return Activated if checkPaperless returns Activated" in {
      mockCheckPaperlessActivated(testToken)

      await(TestPreferencesService.checkPaperless(testToken)).right.value shouldBe Activated
    }

    "return Unset if checkPaperless returns Unset" in {
      mockCheckPaperlessUnset(testToken)

      await(TestPreferencesService.checkPaperless(testToken)).right.value shouldBe Unset(testUrl)
    }

    "return a failed future in checkPaperless returns a failed future" in {
      mockCheckPaperlessException(testToken)

      intercept[Exception](await(TestPreferencesService.checkPaperless(testToken))) shouldBe testException
    }

  }

}
