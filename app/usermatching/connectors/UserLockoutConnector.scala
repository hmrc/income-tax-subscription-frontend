/*
 * Copyright 2019 HM Revenue & Customs
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

package usermatching.connectors

import javax.inject.{Inject, Singleton}

import core.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import usermatching.httpparsers.LockoutStatusHttpParser._
import usermatching.models.LockOutRequest

import scala.concurrent.Future

@Singleton
class UserLockoutConnector @Inject()(val appConfig: AppConfig,
                                     val http: HttpClient) {

  def userLockoutUrl(token: String): String = appConfig.userMatchingUrl + UserLockoutConnector.tokenLockoutUri(token)

  def lockoutUser(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    http.POST[LockOutRequest, LockoutStatusResponse](userLockoutUrl(token), LockOutRequest(appConfig.matchingLockOutSeconds))

  def getLockoutStatus(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    http.GET[LockoutStatusResponse](userLockoutUrl(token))

}

object UserLockoutConnector {
  def tokenLockoutUri(token: String): String = s"/lock/$token"
}
