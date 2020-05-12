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
import play.api.test.Helpers.OK
import services.agent.mocks.MockKeystoreService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utilities.UnitTestTrait
import utilities.agent.TestModels

class KeystoreServiceSpec extends UnitTestTrait
  with MockKeystoreService {

  "Keystore service" should {
    "be DIed with the correct session cache object" in {
      val cache = app.injector.instanceOf[SessionCache]
      val config = app.injector.instanceOf[ServicesConfig]
      cache.defaultSource shouldBe config.getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")
      cache.baseUri shouldBe config.baseUrl("session-cache")
      cache.domain shouldBe config.getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))
    }
  }

  "mock keystore service" should {
    object TestKeystore {
      val keystoreService: KeystoreService = MockKeystoreService
    }

    "configure and verify fetch and save business name as specified" in {
      val testBusinessName = BusinessNameModel("my business name")
      mockFetchBusinessNameFromKeyStore(testBusinessName)
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
      mockFetchAllFromKeyStore(testFetchAll)
      for {
        fetched <- TestKeystore.keystoreService.fetchAll()
      } yield {
        fetched shouldBe testFetchAll

        verifyKeystore(fetchAll = 1)
      }
    }

    "configure and verify remove all as specified" in {
      val testDeleteAll = HttpResponse(OK)
      mockDeleteAllFromKeyStore(testDeleteAll)
      for {
        response <- TestKeystore.keystoreService.deleteAll()
      } yield {
        response shouldBe testDeleteAll

        verifyKeystore(fetchAll = 1)
      }
    }

  }

}
