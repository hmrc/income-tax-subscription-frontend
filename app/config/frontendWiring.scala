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

package config

import javax.inject._
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class SessionCache @Inject()(config: ServicesConfig,
                             val http: HttpClient) extends uk.gov.hmrc.http.cache.client.SessionCache{

  lazy val defaultSource: String = config.getConfString("session-cache.income-tax-subscription-frontend.cache", "income-tax-subscription-frontend")

  lazy val baseUri: String = config.baseUrl("session-cache")
  lazy val domain: String = config.getConfString("session-cache.domain", throw new Exception(s"Could not find core.config 'session-cache.domain'"))
}

