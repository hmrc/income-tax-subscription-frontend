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

package services.individual.mocks

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, CryptoWithKeysFromConfig}

trait MockApplicationCrypto extends BeforeAndAfterEach with MockitoSugar {
  this: Suite =>

  val mockApplicationCrypto: ApplicationCrypto = mock[ApplicationCrypto]
  val mockCryptoWithKeysFromConfig: CryptoWithKeysFromConfig = mock[CryptoWithKeysFromConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockApplicationCrypto)
  }

  def mockEncrypt(): Unit = {

    when(mockApplicationCrypto.QueryParameterCrypto) thenReturn {
      mockCryptoWithKeysFromConfig
    }

    when(mockCryptoWithKeysFromConfig.encrypt(any())) thenReturn Crypted("encryptedValue")

  }


}
