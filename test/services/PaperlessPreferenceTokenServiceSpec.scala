/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.test.Helpers._
import services.mocks.TestPaperlessPreferenceTokenService
import utilities.SubscriptionDataKeys.PaperlessPreferenceToken
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

class PaperlessPreferenceTokenServiceSpec extends UnitTestTrait with TestPaperlessPreferenceTokenService {

  "storeNino" should {
    "store the NINO against a generated token when there is not already a token in Subscription Details " in {
      setupMockSubscriptionDetailsSaveFunctions()
      mockFetchPaperlessPreferenceToken(None)
      mockStoreNinoSuccess(testNino)
      val res = TestPaperlessPreferenceTokenService.storeNino(testNino, "test-reference")

      await(res) mustBe a[String]
      verifySubscriptionDetailsSave(PaperlessPreferenceToken, 1)
    }

    "do not store the token when it is already present in Subscription Details " in {
      mockFetchPaperlessPreferenceToken(Some(testToken))

      val res = TestPaperlessPreferenceTokenService.storeNino(testNino, "test-reference")

      await(res) mustBe testToken
    }
  }
}
