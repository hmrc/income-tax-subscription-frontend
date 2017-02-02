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

package config

import javax.inject._

import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class FrontendAuditConnector extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object FrontendAuditConnector extends FrontendAuditConnector

@Singleton
class WSHttp extends uk.gov.hmrc.play.http.ws.WSHttp with AppName with RunMode {
  override val hooks = NoneRequired
}

@Singleton
class FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = new WSHttp()
}

@Singleton
class SessionCache @Inject()(val http: WSHttp) extends uk.gov.hmrc.http.cache.client.SessionCache with AppName with ServicesConfig {
  override lazy val defaultSource: String = getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")

  override lazy val baseUri = baseUrl("session-cache")
  override lazy val domain = getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))
}

