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

package services.agent

import config.SessionCache
import models.agent.BusinessNameModel
import org.scalatest.Matchers._
import services.agent.mocks.MockKeystoreService
import uk.gov.hmrc.http.HttpResponse
import utilities.UnitTestTrait
import utilities.agent.TestModels

class KeystoreServiceSpec extends UnitTestTrait
  with MockKeystoreService {

  "Keystore service" should {
    "be DIed with the correct session cache object" in {
      val cache = app.injector.instanceOf[SessionCache]
      cache.defaultSource shouldBe cache.getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")
      cache.baseUri shouldBe cache.baseUrl("session-cache")
      cache.domain shouldBe cache.getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))
    }
  }

  "mock keystore service" should {
    object TestKeystore {
      val keystoreService: KeystoreService = MockKeystoreService
    }

    "configure and verify fetch and save business name as specified" in {
      val testBusinessName = BusinessNameModel("my business name")
      setupMockKeystore(fetchBusinessName = testBusinessName)
      for {
        businessName <- TestKeystore.keystoreService.fetchBusinessName()
        _ <- TestKeystore.keystoreService.saveBusinessName(testBusinessName)
      } yield {
        businessName shouldBe testBusinessName

        verifyKeystore(
          fetchBusinessName = 1,
          saveBusinessName = 1
        )
      }
    }

    "configure and verify fetch all as specified" in {
      val testFetchAll = TestModels.emptyCacheMap
      setupMockKeystore(fetchAll = testFetchAll)
      for {
        fetched <- TestKeystore.keystoreService.fetchAll()
      } yield {
        fetched shouldBe testFetchAll

        verifyKeystore(fetchAll = 1)
      }
    }

    "configure and verify remove all as specified" in {
      val testDeleteAll = HttpResponse(200)
      setupMockKeystore(deleteAll = testDeleteAll)
      for {
        response <- TestKeystore.keystoreService.deleteAll()
      } yield {
        response shouldBe testDeleteAll

        verifyKeystore(fetchAll = 1)
      }
    }

  }

}
