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

import com.google.inject.AbstractModule
import config.{AppConfig, FrontendAppConfig}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws._

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[SessionCache]).to(classOf[config.SessionCache]).asEagerSingleton()
    bind(classOf[WSGet]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[HttpGet]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[WSPost]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[HttpPost]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[WSDelete]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[HttpDelete]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[WSPut]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[HttpPut]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[HttpPatch]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[WSHttp]).to(classOf[config.WSHttp]).asEagerSingleton()
    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig]).asEagerSingleton()

    bind(classOf[AuthConnector]).to(classOf[config.AuthConnector])
    bind(classOf[AuditConnector]).to(classOf[config.FrontendAuditConnector])
  }

}
