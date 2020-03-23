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

package config

import javax.inject._
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter

@Singleton
class SessionCache @Inject()(environment: Environment,
                             configuration: Configuration,
                             val http: HttpClient) extends uk.gov.hmrc.http.cache.client.SessionCache with AppName with ServicesConfig {
  override lazy val mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  override protected def appNameConfiguration: Configuration = configuration

  lazy val defaultSource: String = getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")

  lazy val baseUri: String = baseUrl("session-cache")
  lazy val domain: String = getConfString("session-cache.domain", throw new Exception(s"Could not find core.config 'session-cache.domain'"))
}


@Singleton
class ITSAHeaderCarrierForPartialsConverter @Inject()(sessionCookieCrypto: SessionCookieCrypto) extends HeaderCarrierForPartialsConverter {

  def encryptCookieString(cookie: String): String = {
    sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
  }

  override val crypto: String => String = encryptCookieString
}
