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

import play.api.Configuration
import scala.jdk.CollectionConverters.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.*

import javax.inject.{Inject, Singleton}

@Singleton
class ComposeCryptoService @Inject()(configuration: Configuration) {

  private val config = configuration.underlying

  private def stringList(path: String): Seq[String] =
    if (config.hasPath(path)) config.getStringList(path).asScala.toSeq else Seq.empty

  private val currentLegacyAesKey = config.getString("json.encryption.key")
  private val previousLegacyAesKeys = stringList("json.encryption.previousKeys")

  private val currentAesGcmKey = config.getString("json.encryption.gcm.key")
  private val previousAesGcmKeys = stringList("json.encryption.gcm.previousKeys")

  private val legacyCrypto = composeCrypto(
    aesCrypto(currentLegacyAesKey),
    previousLegacyAesKeys.map(aesCrypto)
  )

  private val gcmCrypto = composeCrypto(
    aesGcmCrypto(currentAesGcmKey),
    previousAesGcmKeys.map(aesGcmCrypto)
  )

  val legacyCryptoWithFallback: Encrypter & Decrypter = composeCrypto(
    aesCrypto(currentLegacyAesKey),
    previousLegacyAesKeys.map(aesCrypto) ++ Seq(gcmCrypto)
  )

  val gcmCryptoWithFallback: Encrypter & Decrypter = composeCrypto(
    aesGcmCrypto(currentAesGcmKey),
    previousAesGcmKeys.map(aesGcmCrypto) ++ Seq(legacyCrypto)
  )
}