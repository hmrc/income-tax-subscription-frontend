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

package digitalcontact.services

import digitalcontact.services.mocks.TestPaperlessPreferenceTokenService
import play.api.test.Helpers._
import utils.TestConstants._
import utils.UnitTestTrait

class PaperlessPreferenceTokenServiceSpec extends UnitTestTrait with TestPaperlessPreferenceTokenService {
  "storeNino" should {
    "store the NINO against a generated token when there is not already a token in keystore" in {
      setupMockKeystore(fetchPaperlessPreferenceToken = None)
      mockStoreNinoSuccess(testNino)
      val res = TestPaperlessPreferenceTokenService.storeNino(testNino)

      await(res) mustBe a[String]
      verifyKeystore(savePaperlessPreferenceToken = 1)
    }

    "do not store the token when it is already present in keystore" in {
      setupMockKeystore(fetchPaperlessPreferenceToken = testToken)

      val res = TestPaperlessPreferenceTokenService.storeNino(testNino)

      await(res) mustBe testToken
    }
  }
}
