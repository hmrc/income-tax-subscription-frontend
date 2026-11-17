/*
 * Copyright 2026 HM Revenue & Customs
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

package crypto

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesGcmCryptoFromConfig

class ComposeCryptoServiceSpec extends PlaySpec with Matchers with GuiceOneAppPerSuite {

  private val applicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  private val composeCryptoService = new ComposeCryptoService(app.configuration, applicationCrypto)

  "composedJsonCrypto" should {
    "encrypt with aes-gcm and decrypt the encrypted value" in {
      val plaintext = PlainText("test value")
      val encrypted = composeCryptoService.composedJsonCrypto.encrypt(plaintext)
      val aesGcmCrypto = aesGcmCryptoFromConfig("json.encryption.gcm", app.configuration.underlying)

      aesGcmCrypto.decrypt(encrypted) mustBe plaintext
      composeCryptoService.composedJsonCrypto.decrypt(encrypted) mustBe plaintext
    }

    "decrypt values encrypted with the previous deprecated JsonCrypto" in {
      val plaintext = PlainText("test value")
      val encryptedWithPreviousCrypto = applicationCrypto.JsonCrypto.encrypt(plaintext)

      composeCryptoService.composedJsonCrypto.decrypt(encryptedWithPreviousCrypto) mustBe plaintext
    }
  }
}
