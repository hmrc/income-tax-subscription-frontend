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

package services

import config.AppConfig
import connectors.usermatching.UserLockoutConnector
import connectors.usermatching.httpparsers.LockoutStatusHttpParser.LockoutStatusResponse
import models.usermatching.{LockoutStatus, LockoutStatusFailure, NotLockedOut}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class LockoutUpdate(status: LockoutStatus, updatedCount: Option[Int])

@Singleton
class UserLockoutService @Inject()(appConfig: AppConfig,
                                   userLockoutConnector: UserLockoutConnector,
                                   subscriptionDetailsService: SubscriptionDetailsService) extends Logging {

  def getLockoutStatus(token: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] = {
    val encodedToken = encodeToken(token)

    logger.debug(s"Getting lockout status for token=$token encoded=$encodedToken")
    userLockoutConnector.getLockoutStatus(encodedToken)
  }

  def incrementLockout(token: String, currentFailedMatches: Int)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[LockoutStatusFailure, LockoutUpdate]] = {
    val encodedToken = encodeToken(token)
    val incrementedFailedMatches = currentFailedMatches + 1
    if (incrementedFailedMatches < appConfig.matchingAttempts) {
      Future.successful(Right(LockoutUpdate(NotLockedOut, Some(incrementedFailedMatches))))
    } else {
      userLockoutConnector.lockoutUser(encodedToken) map {
        case Right(status) => Right(LockoutUpdate(status, None))
        case Left(failure) => Left(failure)
      }
    }
  }

  private def encodeToken(token: String): String = URLEncoder.encode(token, "UTF-8")

}
