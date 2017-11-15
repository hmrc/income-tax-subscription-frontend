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

package usermatching.services

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import core.audit.Logging
import uk.gov.hmrc.http.{HeaderCarrier, UserId}
import usermatching.connectors.UserLockoutConnector
import usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse

import scala.concurrent.Future

@Singleton
class UserLockoutService @Inject()(userLockoutConnector: UserLockoutConnector,
                                   logging: Logging) {


  def lockoutUser(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] = {
    val encodedToken = encodeToken(token)

    logging.debug(s"Creating a lock for token=$token encoded=$encodedToken")
    userLockoutConnector.lockoutUser(encodedToken)
  }

  def getLockoutStatus(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] = {
    val encodedToken = encodeToken(token)

    logging.debug(s"Getting lockout status for token=$token encoded=$encodedToken")
    userLockoutConnector.getLockoutStatus(encodedToken)
  }

  private def encodeToken(token: String): String = URLEncoder.encode(token, "UTF-8")
}
