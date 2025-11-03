/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.usermatching

import config.AppConfig
import connectors.usermatching.httpparsers.LockoutStatusHttpParser._
import models.usermatching.LockOutRequest
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserLockoutConnector @Inject()(val appConfig: AppConfig,
                                     val http: HttpClientV2)
                                    (implicit ec: ExecutionContext) {

  def userLockoutUrl(token: String): String = appConfig.userMatchingUrl + UserLockoutConnector.tokenLockoutUri(token)

  def lockoutUser(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
  http.post(url"${userLockoutUrl(token)}").withBody(Json.toJson(LockOutRequest(appConfig.matchingLockOutSeconds))).execute[LockoutStatusResponse]

  def getLockoutStatus(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
  http.get(url"${userLockoutUrl(token)}").execute[LockoutStatusResponse]

}

object UserLockoutConnector {
  def tokenLockoutUri(token: String): String = s"/lock/$token"
}
